package com.company;

import com.stirante.lolclient.ApiResponse;
import com.stirante.lolclient.ClientApi;
import com.stirante.lolclient.ClientConnectionListener;
import com.stirante.lolclient.ClientWebSocket;
import com.stirante.lolclient.libs.org.apache.http.conn.HttpHostConnectException;
import generated.*;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Main {

    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("dd-MM-yyyy");
    // 0 [GET]   Get playable champions in inventory (all owned and frees)
    // 1 [GET]   Check for match found
    // 2 [POST]  Accept the match
    // 3 [GET]   Get picking session info
    // 4 [POST]  Send selection actions
    // 5 [POST]  Send lock actions
    private static final String[] APIs = {
            "/lol-champions/v1/owned-champions-minimal",
            "/lol-matchmaking/v1/ready-check",
            "/lol-matchmaking/v1/ready-check/accept",
            "/lol-champ-select/v1/session",
            "/lol-champ-select/v1/session/actions",
            "/lol-champ-select/v1/session/actions/complete",
    };

    private static final String clientPath = "D:\\Games\\Garena\\Garena\\games\\32787\\LeagueClient";

    private static void getChampionCollectionOfSummoner() {
        //Initialize API
        ClientApi api = new ClientApi(clientPath);
        api.addClientConnectionListener(new ClientConnectionListener() {
            @Override
            public void onClientConnected() {
                try {
                    //Check if user is logged in
                    if (!api.isAuthorized()) {
                        System.out.println("Not logged in!");
                        return;
                    }
                    //Get current summoner
                    LolSummonerSummoner summoner = api.executeGet("/lol-summoner/v1/current-summoner", LolSummonerSummoner.class).getResponseObject();
                    //Get champion collection of summoner
                    LolChampionsCollectionsChampion[] champions = api.executeGet(
                            "/lol-champions/v1/inventories/" + summoner.summonerId + "/champions",
                            LolChampionsCollectionsChampion[].class).getResponseObject();
                    for (LolChampionsCollectionsChampion champion : champions) {
                        if (champion.ownership.owned) {
                            System.out.println(champion.name + " purchased on " +
                                    FORMATTER.format(new Date(champion.ownership.rental.purchaseDate)));
                            for (LolChampionsCollectionsChampionSkin skin : champion.skins) {
                                if (!skin.isBase && skin.ownership.owned) {
                                    System.out.println("\t" + skin.name + " purchased on " +
                                            FORMATTER.format(new Date(skin.ownership.rental.purchaseDate)));
                                }
                            }
                        }
                    }
                    api.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClientDisconnected() {

            }
        });
    }

    private static void getCurrentUserChatInfo() {
        //Initialize API
        ClientApi api = new ClientApi(clientPath);
        //Add listener, which will notify us about client connection available
        api.addClientConnectionListener(new ClientConnectionListener() {
            @Override
            public void onClientConnected() {
                try {
                    //Get current user chat info
                    ApiResponse<LolChatUserResource> user =
                            api.executeGet("/lol-chat/v1/me", LolChatUserResource.class);
                    //Print status message
                    System.out.println(user.getRawResponse().replace(',', '\n'));
                    api.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClientDisconnected() {

            }
        });
    }

    private static String @NotNull [] getChampions() throws IOException {
        //Initialize API
        ClientApi api = new ClientApi(clientPath);
        List<String> data = new ArrayList<String>();
        //Add listener, which will notify us about client connection available
        api.addClientConnectionListener(new ClientConnectionListener() {
            @Override
            public void onClientConnected() {
                try {
                    //Get current user chat info
                    LolChampionsCollectionsChampion[] champions = api.executeGet(APIs[0], LolChampionsCollectionsChampion[].class).getResponseObject();
                    //Print status message
                    for (LolChampionsCollectionsChampion champion : champions) {
                        data.add(champion.alias);
                    }
                    api.stop();
                } catch (HttpHostConnectException e) {
                    System.out.println("Can't connect to client");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClientDisconnected() {

            }
        });

        return data.toArray(new String[0]);
    }

    private static void getMatchStatus() throws IOException {
        //Initialize API
        ClientApi api = new ClientApi(clientPath);
        //Add listener, which will notify us about client connection available
        api.addClientConnectionListener(new ClientConnectionListener() {
            @Override
            public void onClientConnected() {
                try {
                    LolMatchmakingMatchmakingReadyCheckResource status;
                    do {
                        //Get current user chat info
                        status = api.executeGet(APIs[1], LolMatchmakingMatchmakingReadyCheckResource.class).getResponseObject();
                        //Print status message
                    }
                    while (status == null || !status.state.toString().equals("INPROGRESS"));
                    api.stop();
                } catch (HttpHostConnectException e) {
                    System.out.println("Can't connect to client");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClientDisconnected() {

            }
        });
    }

    private static void acceptMatch() throws IOException {
        //Initialize API
        ClientApi api = new ClientApi(clientPath);
        //Add listener, which will notify us about client connection available
        api.addClientConnectionListener(new ClientConnectionListener() {
            @Override
            public void onClientConnected() {
                try {
                    api.executePost(APIs[2]);
                    api.stop();
                } catch (HttpHostConnectException e) {
                    System.out.println("Can't connect to client");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClientDisconnected() {

            }
        });
    }

    private static ClientWebSocket socket;

    /**
     * Simple example showing how to receive websocket events from client
     */
    public static void printEvent() throws Exception {
        //Initialize API
        ClientApi api = new ClientApi(clientPath);
        api.addClientConnectionListener(new ClientConnectionListener() {
            @Override
            public void onClientConnected() {
                System.out.println("Client connected");
                try {
                    //open web socket
                    socket = api.openWebSocket();
                    //add event handler, which prints every received event
                    socket.setSocketListener(new ClientWebSocket.SocketListener() {
                        @Override
                        public void onEvent(ClientWebSocket.Event event) {
                            System.out.println(event);
//                            if (event.getData() instanceof LolClashPlaymodeRestrictedInfo dat) {
//                                System.out.println(dat.isRestricted);
//                                System.out.println(dat.phaseId);
//                                System.out.println(dat.presenceState);
//                                System.out.println(dat.readyForVoice);
//                                System.out.println(dat.rosterId);
//                                System.out.println(dat.tournamentId);
//                            }
                        }

                        @Override
                        public void onClose(int code, String reason) {
                            System.out.println("Socket closed, reason: " + reason);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClientDisconnected() {
                System.out.println("Client disconnected");
                socket.close();
            }
        });
        //close socket when user enters something into console
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        reader.readLine();
        api.stop();
        socket.close();
    }

    public static void main(String[] args) throws Exception {
        getMatchStatus();
        acceptMatch();
    }
}
