package idv.henry.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@ServerEndpoint("/socketserver")
@Component
public class SocketEndpoint {
	//TODO 計算墩數  
	//TODO 打牌   
	//TODO 計算勝利
	
	private static LinkedHashMap<SocketEndpoint,String> webSocketMap = new LinkedHashMap<SocketEndpoint,String>();
	private Session session;
	private static int bidbase = 1;//每一輪的第一墩
	private static int trick_even;
	private static int trick_odd;
	private static int trump;
	private static List<String> playerJoined= new ArrayList<String>();
	private static int evenTeam = 0;
	private static int oddTeam = 0;
	private static HashSet<String> passSet = new HashSet<String>();
	private static LinkedHashMap<String,String> playMap = new LinkedHashMap<String,String>();
	@OnMessage
	public void onMessage(String message, Session session) throws Exception {
		System.out.println("從您的網頁收到一封訊息: 「" + message + "」");
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, String> map = mapper.readValue(message, new TypeReference<HashMap<String, String>>(){});
//		System.out.println("deal".equals(map.get("status")));
		/*DEAL***************************/
		if ("deal".equals(map.get("status"))) {
			List<String> list = new ArrayList<String>();
			list = deal();
			int i=0;
			for (SocketEndpoint item : webSocketMap.keySet()) {
				try {
					if (i > list.size())
						break;
					item.sendMessage(list.get(i));
					i++;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			/*INIT**************************/
		} else if("init".equals(map.get("status"))){
			//TODO say goodbye to other people when 4 people ready.
			//TODO check your cards if good enough or not
			//TODO lock suit if player can follow first player
			webSocketMap.put(this, map.get("name"));
			HashMap<String, String> signinMap = new HashMap<String, String>();
			signinMap.put("status", "signin");
			signinMap.put("name", map.get("name"));
			playerJoined.add(map.get("name"));
			System.out.println(playerJoined.toString());
			System.out.println(playerJoined.indexOf(map.get("name")) + "");
			signinMap.put("seat", playerJoined.indexOf(map.get("name")) + "");
			for (SocketEndpoint item : webSocketMap.keySet()) {
				try {
					message = mapper.writeValueAsString(signinMap);
					item.sendMessage(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(playerJoined.size()==4) {
				
				HashMap<String, String> readyMap = new HashMap<String, String>();
				readyMap.put("status", "ready");
				readyMap.put("message", "Ready to play");
				for (SocketEndpoint item : webSocketMap.keySet()) {
					try {
						message = mapper.writeValueAsString(readyMap);
						item.sendMessage(message);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			/*BID**************************/
		} else if("bid".equals(map.get("status"))){
			HashMap<String, String> bidMap = new HashMap<String, String>();
			bidMap.put("name", map.get("name"));
			bidMap.put("bid", map.get("bid"));
			bidMap.put("status", "bid");
			bidMap.put("next", (playerJoined.indexOf(map.get("name")) + 1) % 4 + "");
			if ("P".equals(map.get("bid"))) {
				System.out.println("PASS!");
				bidMap.put("bidbase", bidbase + "");
				passSet.add(map.get("name"));
				System.out.println(passSet.toString());
				
			} else {
//				bidMap.put("name", map.get("name"));
//				bidMap.put("bid", map.get("bid"));
				bidbase=Integer.parseInt(map.get("bid")) + 1;
				bidMap.put("bidbase", (Integer.parseInt(map.get("bid")) + 1) + "");
				passSet.clear();
			}
			System.out.println(bidMap.toString());
			message = mapper.writeValueAsString(bidMap);
			for (SocketEndpoint item : webSocketMap.keySet()) {
				try {
					item.sendMessage(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (passSet.size() == 3) {
				String nextPlayer = "";
				for (String player : playerJoined) {
					if (!passSet.contains(player))
						nextPlayer = player;
				}
				bidMap.clear();
				setGoal(bidbase - 1, nextPlayer);// 前一墩為合約墩數
				bidMap.put("status", "contract");
				bidMap.put("trump", trump + "");
				bidMap.put("oddTrick", trick_odd + "");
				bidMap.put("evenTrick", trick_even + "");
				bidMap.put("next", (playerJoined.indexOf(nextPlayer) + 1) % 4 + "");
				message = mapper.writeValueAsString(bidMap);
				for (SocketEndpoint item : webSocketMap.keySet()) {
					try {
						item.sendMessage(message);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			/*PLAY**************************/
		} else if("play".equals(map.get("status"))){
			playMap.put(map.get("card"), map.get("name"));
			HashMap<String, String> trickMap = new HashMap<String, String>();
			trickMap.putAll(map);
			trickMap.put("card", map.get("card"));
			trickMap.put("status", "trick");
			trickMap.put("player", (playerJoined.indexOf(map.get("name"))) % 4 + "");
//			message = mapper.writeValueAsString(trickMap);
//			for (SocketEndpoint item : webSocketMap.keySet()) {
//				try {
//					item.sendMessage(message);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}

			if (playMap.size() == 4) {
				String winner = compareCard(playMap);
				System.out.println("winner is ......"+winner);
				System.out.println("winner is ......"+playerJoined.indexOf(winner));
				if (playerJoined.indexOf(winner) % 2 == 0)
					oddTeam++;
				else
					evenTeam++;
				trickMap.put("evenTeam", evenTeam+"");
				trickMap.put("oddTeam", oddTeam+"");
				trickMap.put("next", (playerJoined.indexOf(winner) ) % 4 + "");
				
				playMap.clear();
				
				if (oddTeam == trick_odd || evenTeam == trick_even) {
					String winTeam = oddTeam == trick_odd ? "oddTeam" : "evenTeam";
					System.out.println("winner is ......" + winTeam);
					trickMap.put("next", winTeam);
				}
			}else {
				
				trickMap.put("next", (playerJoined.indexOf(map.get("name")) + 1) % 4 + "");
			}
			message = mapper.writeValueAsString(trickMap);
			for (SocketEndpoint item : webSocketMap.keySet()) {
				try {
					item.sendMessage(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			for (SocketEndpoint item : webSocketMap.keySet()) {
				try {
					item.sendMessage(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	

	private String compareCard(LinkedHashMap<String, String> playMap) {
		//1:Club 2:Diamond 3:Heart 4:Spade 
		String firstCard = (String) playMap.keySet().toArray()[0];
		String trumpCard = "";

		String winner = firstCard;
		
		System.out.println("++++firstCard+++" + firstCard + "===trump====" +trump);
		//同花色比較
		for (String card : playMap.keySet()) {
			String winnerSuit = winner.substring(0, 1);
			int winnerNum = "01".equals(winner.substring(1)) ? 14 : Integer.parseInt(winner.substring(1));
			String suit = card.substring(0, 1);
			int num = "01".equals(card.substring(1)) ? 14 : Integer.parseInt(card.substring(1));
			System.out.println("+++++++" + suit + "=======" + num);

			if (winnerSuit.equals(suit)) {
				if (num >= winnerNum) {
					winner = card;
					System.out.println("@@@@@" + winner);
				}
			}
			if ((trump + "").equals(suit) && trumpCard == "") {
				trumpCard = card;
				System.out.println("@@trumpCard@@@" + trumpCard);
			}
		}
		if (trumpCard != "") {
			// 王牌花色比較
			int trumpCardNum = "01".equals(trumpCard.substring(1)) ? 14 : Integer.parseInt(trumpCard.substring(1));

			for (String card : playMap.keySet()) {
				String suit = card.substring(0, 1);
				int num = "01".equals(card.substring(1)) ? 14 : Integer.parseInt(card.substring(1));
				System.out.println("+++++++" + suit + "=======" + num);

				if ((trump + "").equals(suit)) {
					if (num >= trumpCardNum) {
						winner = card;
						System.out.println("@@@@@" + winner);
					}
				}

			}
			System.out.println("######" + winner);
		}
		
		return playMap.get(winner);
	}



	@OnOpen
	public void onOpen(Session session) {
        this.session = session;
        webSocketMap.put(this, "");
		System.out.println("與客戶端建立連線了！");
	}

	@OnClose
	public void onClose() {
		webSocketMap.remove(this);
		playerJoined.clear();
		passSet.clear();
		System.out.println("連線關閉！");
	}

	@OnError
	public void onError(Session session, Throwable t) {
		System.out.println("連線發生錯誤！");
		System.out.println(t.toString());
	}
	
	public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }
	
	public List<String> deal() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
//		List<String> list = new ArrayList<String>();
		//1:Club 2:Diamond 3:Heart 4:Spade 
		String[] arr = {
				"101", "102", "103", "104", "105", "106", "107", "108", "109", "110", "111", "112", "113", 
				"201", "202", "203", "204", "205", "206", "207", "208", "209", "210", "211", "212", "213", 
				"301", "302", "303", "304", "305", "306", "307", "308", "309", "310", "311", "312", "313", 
				"401", "402", "403", "404", "405", "406", "407", "408", "409", "410", "411", "412", "413", 
				};
        List<String> list = Arrays.asList(arr);
		Collections.shuffle(list);
		List<String> rtnList = new ArrayList<String>();
		for (int i = 0; i < 4; i++) {
			List<String> tempList = list.subList(13 * i, 13 * (1 + i));
			System.out.println("Before:"+tempList);
			//tempList.sort(String::compareTo);
			tempList.sort((s1, s2) -> {
				Integer i1=Integer.parseInt(s1);
				Integer i2=Integer.parseInt(s2);
				return i1.compareTo(i2);
				
			});
			rtnList.add(objectMapper.writeValueAsString(tempList));
			System.out.println("After:"+tempList);
		}
		return rtnList;
	}
	
	private void setGoal(int bid, String player) {
		System.out.println(bid);
		int trick;
		trump = bid % 5; // 1:Club 2:Diamond 3:Heart 4:Spade 5:NoTrump
//		System.out.println(bid / 5.0);//整數除以整數得整數
		System.out.println(Math.ceil(bid / 5.0));
//		System.out.println(Math.floor(bid / 5.0));
		trick = ((int) Math.ceil(bid / 5.0)) + 6;
		//南北玩家 is odd 東西玩家 is even
		if ((playerJoined.indexOf(player) + 1) % 2 == 0) {
			trick_even = trick;
			trick_odd = 14 - trick;
		} else {
			trick_odd = trick;
			trick_even = 14 - trick;
		}
		
		
		System.out.println("GOAL:" + trick + "====" + trump);
	}
}
