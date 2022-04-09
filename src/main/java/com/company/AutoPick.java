package com.company;

import com.stirante.lolclient.ApiResponse;
import com.stirante.lolclient.ClientApi;
import com.stirante.lolclient.ClientConnectionListener;
import com.stirante.lolclient.ClientWebSocket;
import com.stirante.lolclient.libs.com.google.gson.JsonObject;
import com.stirante.lolclient.libs.com.google.gson.internal.LinkedTreeMap;
import com.stirante.lolclient.libs.org.apache.http.conn.HttpHostConnectException;
import generated.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AutoPick {

    public static boolean autoAccept = true;
    public static boolean autoPick = true;
    public static boolean autoLock = true;
    public static boolean accepted = false;
    public static boolean picked = false;
    public static boolean locked = false;
    public static int selectedChampionId = -1;

    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("dd-MM-yyyy");

    private static final String clientPath = "D:\\Games\\Garena\\Garena\\games\\32787\\LeagueClient";

    private static final ClientApi api = new ClientApi(clientPath);

    private static ClientWebSocket socket;

    // 0 [GET]   Get playable champions in inventory (all owned and frees)
    // 1 [GET]   Check for match found
    // 2 [POST]  Accept the match
    // 3 [GET]   Get picking session info
    // 4 [POST]  Send selection actions
    private static final String[] APIs = {
            "/lol-champions/v1/owned-champions-minimal",
            "/lol-matchmaking/v1/ready-check",
            "/lol-matchmaking/v1/ready-check/accept",
            "/lol-champ-select/v1/session",
            "/lol-champ-select/v1/session/actions"
    };


    /**
     * Simple example showing how to receive websocket events from client
     */
    public static void run() {
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
                            if (event.getData() instanceof LolMatchmakingMatchmakingReadyCheckResource dat) {
                                if (dat.state.toString().equals("INPROGRESS")) {
                                    try {
                                        if (AutoPick.autoAccept) {
                                            api.executePost(APIs[2]);
                                            System.out.println("Accepted");
                                        }
                                        AutoPick.picked = false;
                                    } catch (HttpHostConnectException e) {
                                        System.out.println("Can't connect to client");
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else if (event.getData() instanceof LolChampSelectChampSelectSession dat) {
                                if (!AutoPick.picked) {
                                    int id = getId(dat);
                                    if (id > -1) {
                                        String pickApi = APIs[4] + '/' + id;
                                        String lockApi = pickApi + "/complete";
                                        if (AutoPick.autoPick && AutoPick.selectedChampionId > -1 && !AutoPick.picked) {
//                                            String json = "{\"championId\":" + AutoPick.selectedChampionId + "}";
                                            JsonObject json = new JsonObject();
                                            json.addProperty("championId", AutoPick.selectedChampionId);
                                            try {
                                                ApiResponse<?> response = api.executePatch(pickApi, json);
                                                System.out.println("Selected");
                                                AutoPick.picked = true;
                                            } catch (HttpHostConnectException e) {
                                                System.out.println("Can't connect to client");
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        if (AutoPick.autoLock) {
                                            try {
                                                ApiResponse<?> response = api.executePost(lockApi);
                                                System.out.println(response.getRawResponse());
                                                System.out.println("Locked");
                                                AutoPick.locked = true;
                                            } catch (HttpHostConnectException e) {
                                                System.out.println("Can't connect to client");
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            }
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
        try {
            reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        api.stop();
        socket.close();
    }

    private static void getChampionId(String championName) {
        api.addClientConnectionListener(new ClientConnectionListener() {
            @Override
            public void onClientConnected() {
                try {
                    LolChampionsCollectionsChampion[] champions = getChampions();
                    if (champions != null) {
                        String selectedChampionName = championName;
                        selectedChampionName = selectedChampionName.replaceAll("\\s+", "");
                        selectedChampionName = selectedChampionName.toLowerCase(Locale.ENGLISH);
                        for (LolChampionsCollectionsChampion champion : champions) {
                            String tmp = champion.alias;
                            tmp = tmp.toLowerCase(Locale.ENGLISH);
                            if (tmp.equals(selectedChampionName)) {
                                tmp = champion.name;
                                if (tmp.equals("7")) {
                                    AutoPick.selectedChampionId = -1;
                                }
                                AutoPick.selectedChampionId = champion.id;
                                System.out.println("Selected " + championName);
                                return;
                            }
                        }
                    }
                    AutoPick.selectedChampionId = -2;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClientDisconnected() {

            }
        });
    }

    private static LolChampionsCollectionsChampion[] getChampions() {
        LolChampionsCollectionsChampion[] champions;
        try {
            champions = api.executeGet(APIs[0], LolChampionsCollectionsChampion[].class).getResponseObject();
            return champions;
        } catch (HttpHostConnectException e) {
            System.out.println("Can't connect to client");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static int getId(LolChampSelectChampSelectSession dat) {
        Long myCellId = dat.localPlayerCellId;
        if (myCellId >= 0) {
            List<Object> actions = dat.actions;
            List actions0 = (List) actions.get(0);
            for (Object o : actions0) {
                LinkedTreeMap action = (LinkedTreeMap) o;
                if (myCellId.intValue() == ((Double) action.get("actorCellId")).intValue()) {
                    return ((Double) action.get("id")).intValue();
                }
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        getChampionId("masteryi");
        run();
    }
}
