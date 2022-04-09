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
     * Main run for this tool
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
                            // auto accept
                            if (event.getData() instanceof LolMatchmakingMatchmakingReadyCheckResource dat) {
                                if (dat.state.toString().equals("INPROGRESS")) {
                                    try {
                                        if (AutoPick.autoAccept && !AutoPick.accepted) {
                                            api.executePost(APIs[2]);
                                            AutoPick.accepted = true;
                                            System.out.println("Accepted");
                                            AutoPick.picked = false;
                                            AutoPick.locked = false;
                                        }
                                    } catch (HttpHostConnectException e) {
                                        System.out.println("Can't connect to client");
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else if (event.getData() instanceof LolChampSelectChampSelectSession dat) {
                                int id = getId(dat);
                                if (id > -1) {
                                    String pickApi = APIs[4] + '/' + id;
                                    String lockApi = pickApi + "/complete";
                                    // auto pick
                                    if (AutoPick.autoPick && AutoPick.selectedChampionId > -1 && !AutoPick.picked) {
                                        JsonObject json = new JsonObject();
                                        json.addProperty("championId", AutoPick.selectedChampionId);
                                        try {
                                            ApiResponse<?> response = api.executePatch(pickApi, json);
                                            if (response == null) {
                                                System.out.println("Selected");
                                                AutoPick.picked = true;
                                                AutoPick.accepted = false;
                                            }
                                        } catch (HttpHostConnectException e) {
                                            System.out.println("Can't connect to client");
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    // auto lock
                                    if (AutoPick.autoLock && !AutoPick.locked) {
                                        try {
                                            ApiResponse<?> response = api.executePost(lockApi);
                                            if (response == null) {
                                                System.out.println("Locked");
                                                AutoPick.locked = true;
                                                AutoPick.picked = false;
                                                AutoPick.accepted = false;
                                            }
                                        } catch (HttpHostConnectException e) {
                                            System.out.println("Can't connect to client");
                                        } catch (Exception e) {
                                            e.printStackTrace();
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
                api.stop();
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


    /**
     * Set champion to auto pick
     *
     * @param championName - the name of the champion you want to auto pick
     */
    public static void setSelectChampion(String championName) {
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
                                System.out.println("Selected " + tmp);
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
        int lengh = args.length;
        String arg;
        for (int i = 0; i < lengh; i++) {
            arg = args[i].toLowerCase(Locale.ENGLISH);
            switch (arg) {
                case "--champion", "--cham" -> setSelectChampion(args[++i]);
                case "--pick" -> AutoPick.autoPick = !(args[i + 1].equals("0") || args[i + 1].equals("false"));
                case "--accept" -> AutoPick.autoAccept = !(args[i + 1].equals("0") || args[i + 1].equals("false"));
                case "--lock" -> AutoPick.autoLock = !(args[i + 1].equals("0") || args[i + 1].equals("false"));
            }
        }
        run();
    }
}
