package com.max2341.fruitbin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.mojang.realmsclient.gui.ChatFormatting;

import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import scala.annotation.varargs;

public class Utils {
	public static Set<Character>numbers = new HashSet<Character>(Arrays.asList(new Character[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'}));
	public static Set<Character>letters = new HashSet<Character>(Arrays.asList(new Character[] {' ', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'}));
	public static HashMap<String, Integer>itemAmounts = new HashMap<String, Integer>();
	public static int lastTotalAuctions;
	static Pair<String, Integer> bestAuctionIDAndProfit = new Pair<String, Integer>("404", 0);
	public enum Risk {NO, LOW, MEDIUM, HIGH}
	public static float auctionListTax = 0.01f;
	public static float auctionCollectTax = 0.01f;
	public static boolean DEV_DEBUG = false;
	
	
	public static void print(Object msg) {
		if(msg != null) {
			if (DEV_DEBUG)
				System.out.println(msg);
		}
		else  System.out.println("null");
	}
	
	public static void sendFlip(AuctionInfo auctionInfo) {
		EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
		if(player == null) {
			print("MC player is null");
			return;
		}
		ChatFormatting itemColor = Utils.getColorByRarity(auctionInfo.auction.tier);
		ChatFormatting riskColor = auctionInfo.risk == Risk.NO ? ChatFormatting.AQUA : auctionInfo.risk == Risk.LOW ? ChatFormatting.GREEN : auctionInfo.risk == Risk.MEDIUM ? ChatFormatting.GRAY : ChatFormatting.GRAY;
		IChatComponent comp = new ChatComponentText(itemColor + auctionInfo.auction.item_name + " " + ChatFormatting.WHITE + Utils.GetAbbreviatedFloat(auctionInfo.price) + " -> " + Utils.GetAbbreviatedFloat(auctionInfo.lowestBin) + ChatFormatting.GOLD + 
				" [" + String.format("%,d", auctionInfo.profit) + " coins]" + (auctionInfo.profitPercent >= 50 ? ChatFormatting.AQUA : ChatFormatting.GRAY) +" [" + auctionInfo.profitPercent + "%] " + riskColor + auctionInfo.risk.toString().toUpperCase() + " RISK");
		ChatStyle style = Utils.createClickStyle(ClickEvent.Action.RUN_COMMAND, "/viewauction " + auctionInfo.auction.uuid);
		comp.setChatStyle(style);
		player.addChatMessage(comp);
		print("FOUND FLIP! " + auctionInfo.auction.item_name + " " + auctionInfo.price + " -> " + auctionInfo.lowestBin);
	}
	
	public static long GetUnabbreviatedString(String string) {
		float number;
		long multiplier;
		char[] charArray = string.toLowerCase().toCharArray();
		char lastChar = charArray[string.length() - 1];
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < string.length() - 1; i++) {
			sb.append(charArray[i]);
		}
		if(numbers.contains(lastChar))
			sb.append(lastChar);
		number = Float.parseFloat(sb.toString());
		multiplier = (lastChar == 'k') ? 1000 : (lastChar == 'm') ? 1000000 : (lastChar == 'b') ? 1000000000 : (lastChar == 't') ? 1000000000000l : 1;
		
		return Math.round((number) * multiplier);
	}
	public static String GetAbbreviatedFloat(float number) {
		DecimalFormat df = new DecimalFormat("0.#");
		if(number >= 1000000000) 
			return df.format(Math.round(number / 1000000f)/1000f) + "B";	
		else if(number >= 1000000) 
			return df.format(Math.round(number / 10000f)/100f) + "M";
		else if(number >= 1000) 
			return df.format(Math.round(number / 100f)/10f) + "K";
		else
			return df.format(Math.round(number * 10f)/10f) + "";
	}
	public static ChatFormatting getColorByRarity(String rarity) {
		if(rarity.equalsIgnoreCase("LEGENDARY"))
			return ChatFormatting.GOLD;
		else if (rarity.equalsIgnoreCase("EPIC"))
			return ChatFormatting.DARK_PURPLE;
		else if (rarity.equalsIgnoreCase("RARE"))
			return ChatFormatting.BLUE;
		else if (rarity.equalsIgnoreCase("UNCOMMON"))
			return ChatFormatting.GREEN;
		else if (rarity.equalsIgnoreCase("COMMON"))
			return ChatFormatting.GRAY;
		else if (rarity.equalsIgnoreCase("MYTHIC"))
			return ChatFormatting.LIGHT_PURPLE;
		else if (rarity.equalsIgnoreCase("DIVINE"))
			return ChatFormatting.AQUA;
		else {
			return ChatFormatting.RED;
		}
	}
	public static ChatComponentText getChatMessage(String message) {
		return new ChatComponentText(ChatFormatting.DARK_GRAY + "FruitBin: " + ChatFormatting.RESET + "" + message);
	}
	public static ChatComponentText getChatMessage(String message, boolean reset) {
		return new ChatComponentText(ChatFormatting.DARK_GRAY + "FruitBin: " + (reset ? ChatFormatting.RESET : "") + "" + message);
	}
	public static String getHTML(String urlToRead) throws IOException {
	      URL url = new URL(urlToRead);
	      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	      conn.setRequestMethod("GET");
	      int status = conn.getResponseCode();	
	      
	      Reader streamReader = null;

	      if (status > 299) {
	          streamReader = new InputStreamReader(conn.getErrorStream());
	      } else {
	          streamReader = new InputStreamReader(conn.getInputStream());
	      }
	      BufferedReader in = new BufferedReader( new InputStreamReader(conn.getInputStream()));
			String inputLine;
			StringBuffer content = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
			    content.append(inputLine);
			}
			in.close();
		    return content.toString();
		 }
	static String getFlipSound() {
		return Reference.MODID + ":alerts.flipalert";
	}
	public static HashMap<String, Float> initializeAuctions(String url) throws IOException {
		int totalPages = 1;
		HashMap<String, Float>itemLowestBins = new HashMap<String, Float>();
		HashMap<String, Integer>newItemAmounts = new HashMap<String, Integer>();
		lastTotalAuctions = 0;
		
		for(int page = 0; page < totalPages; page++) {
			String json = null;
			try {
				json = Utils.getHTML(url + "?page=" + page);
			} catch (IOException e) {
				print(e.toString());
			}
			Gson gson = new Gson();
			Auctions auctions = gson.fromJson(json, Auctions.class);
			
			totalPages = auctions.totalPages;
			for(AuctionItem auction : auctions.auctions) {
				NBTCompound extraInfo = NBTReader.readBase64(auction.item_bytes);
				String myID = getMyID(extraInfo);
				
				if(itemLowestBins.containsKey(myID)) {
					if(auction.starting_bid < itemLowestBins.get(myID)) {
						itemLowestBins.put(myID, auction.starting_bid);
					}
				}
				else {
					itemLowestBins.put(myID, auction.starting_bid);
				}
				if(newItemAmounts.containsKey(myID)) {
					int newValue = newItemAmounts.get(myID) + 1;
					newItemAmounts.put(myID, newValue);
				} else newItemAmounts.put(myID, 1);
			}
		}
		itemAmounts = newItemAmounts;
		if(FruitBin.showDebugMessages)
			quickChatMsg("Initialized Auctions", ChatFormatting.GREEN);
		return itemLowestBins;
	}
	
	public static HashMap<String, Float> scan(String url, long budget, int minProfit, HashMap<String, Float>itemLowestBins) {
		if(FruitBin.showDebugMessages)
			quickChatMsg("Scan called", ChatFormatting.GREEN);
		HashMap<String, Float>newItemLowestBins = new HashMap<String, Float>();
		HashMap<String, Integer>newItemAmounts = new HashMap<String, Integer>();
		List<AuctionInfo> result = new ArrayList<AuctionInfo>();
		int newAmountOfAuctions = 0;
		
		long startTime = System.currentTimeMillis();
//		for(AuctionItem auction : prevAuctions) {
//			
//		}
		
		try {
			Gson gson = new Gson();
//			List<AuctionInfo>auctionInfos = new ArrayList<AuctionInfo>();

			int totalPages = 1;
			int totalFlips = 0;
			for(int page = 0; page < totalPages; page++) {
				String newjson = Utils.getHTML(url + "?page=" + page);
				Auctions newAuctions = gson.fromJson(newjson, Auctions.class);
				totalPages = newAuctions.totalPages;
				
				if(page == 0) {
					if(newAuctions.totalAuctions == lastTotalAuctions) {
						if(FruitBin.showDebugMessages)
							quickChatMsg("API not refreshed", ChatFormatting.RED);

						return null;
					}
					if(FruitBin.showDebugMessages)
						quickChatMsg("Started Filtering", ChatFormatting.GREEN);
				}				

				for (AuctionItem auction : newAuctions.auctions) {
					NBTCompound extraInfo = NBTReader.readBase64(auction.item_bytes);
					if (auction.bin && auction.starting_bid <= budget && !auction.claimed) {
						//HashMap<String, Integer> enchantments = (HashMap<String, Integer>) extraInfo.get("enchantments");
						
														
						String myID = getMyID(extraInfo);
						
						float minPrice;
						if(itemLowestBins.containsKey(myID))
							minPrice = itemLowestBins.get(myID);
						else minPrice = 0;
						
						float sellPrice = minPrice * (1 - auctionListTax - ((minPrice >= 1000000) ? auctionCollectTax : 0)) - 100;
						float profitPercent = ((sellPrice / auction.starting_bid) - 1) * 100;
						if (auction.starting_bid <= sellPrice - minProfit && profitPercent >= FruitBin.minProfitPercent) {
							
							int profit = (int)(sellPrice - auction.starting_bid);
							Risk risk;
							int amount;
							if (itemAmounts.containsKey(myID))
								amount = itemAmounts.get(myID);
							else
								amount = 0;
							if (amount < 5) 
								risk = Risk.HIGH;
							else if (amount < 25)
								risk = Risk.MEDIUM;
							else if (amount < 100) 
								risk = Risk.LOW;
							else 
								risk = Risk.NO;
							if (risk.ordinal() <= FruitBin.maxRisk.ordinal()) {
								AuctionInfo info = new AuctionInfo(auction, profit, Math.round(profitPercent), auction.starting_bid, itemLowestBins.get(myID), risk);
//								auctionInfos.add(info);
								if(info.profit > bestAuctionIDAndProfit.value) {
									bestAuctionIDAndProfit.key = auction.uuid;
									bestAuctionIDAndProfit.value = info.profit;
								}
								if(totalFlips < 3) {
									ISound sound = new PositionedSound(new ResourceLocation(getFlipSound())){};
									Minecraft.getMinecraft().getSoundHandler().playSound(sound);
								}
								totalFlips++;
								sendFlip(info);
							}
						}
					}
					//LOWESTBIN & ITEMAMOUNTS
					String myID = getMyID(extraInfo);
					
					if(newItemLowestBins.containsKey(myID)) {
						if(auction.starting_bid < newItemLowestBins.get(myID)) {
							newItemLowestBins.put(myID, auction.starting_bid);
						}
					}
					else {
						newItemLowestBins.put(myID, auction.starting_bid);
					}
					if(newItemAmounts.containsKey(myID)) {
						int newValue = newItemAmounts.get(myID) + 1;
						newItemAmounts.put(myID, newValue);
					} else newItemAmounts.put(myID, 1);
					newAmountOfAuctions++;
				}
			}
			if(FruitBin.showDebugMessages)
				quickChatMsg("Searched through " + newAmountOfAuctions + " auctions in " 
						+ (System.currentTimeMillis() - startTime) / 1000f + "s and found " + totalFlips + " flips", ChatFormatting.GREEN);

//			prevAuctions = theseAuctions;
			itemAmounts = newItemAmounts;
			lastTotalAuctions = newAmountOfAuctions;
			return newItemLowestBins;
			
//			return auctionInfos.toArray(new AuctionInfo[auctionInfos.size()]);
		} catch (Exception e) {
			if(FruitBin.showDebugMessages)
				quickChatMsg("EXCEPTION: " + e, ChatFormatting.RED);
			print("EXCEPTION: " + e);
			return itemLowestBins;
		}
	}
	public static void openBestAuction() {
		EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
		if(player != null)
			player.sendChatMessage("/viewauction " + bestAuctionIDAndProfit.getKey());
	}
	public static void quickChatMsg(String message, ChatFormatting color) {
		EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
		if(player != null)
			player.addChatMessage(getChatMessage(color + message, false));
		else {
			print("quickchat: mc player is null");
		}
	}
	
	public static String getMyID(NBTCompound bytes) {
		NBTCompound attributes = bytes.getList("i").getCompound(0).getCompound("tag").getCompound("ExtraAttributes");
		String id = attributes.getString("id");
		return id;
	}
	
    public static ChatStyle createClickStyle(ClickEvent.Action action, String value) {
        ChatStyle style = new ChatStyle();
        style.setChatClickEvent(new ClickEvent(action, value));
        style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.GOLD+value)));
        return style;
    }
}


//public static AuctionInfo[] filter(AuctionItem[] auctions, int minProfit) {
//HashMap<String, Float>itemLowestBins = new HashMap<String, Float>();
//HashMap<String, Integer>itemAmounts = new HashMap<String, Integer>();
//List<AuctionInfo> result = new ArrayList<AuctionInfo>();
//List<AuctionItem> newAuctionItems = new ArrayList<AuctionItem>();
//for(AuctionItem auction : prevAuctions) {
//	String myID = getMyIDFromName(auction.item_name);
//	
//	if(itemLowestBins.containsKey(myID)) {
//		if(auction.starting_bid < itemLowestBins.get(myID)) {
//			itemLowestBins.put(myID, auction.starting_bid);
//		}
//	}
//	else {
//		itemLowestBins.put(myID, auction.starting_bid);
//		
//	}
//	if(itemAmounts.containsKey(myID)) {
//		int newValue = itemAmounts.get(myID) + 1;
//		itemAmounts.put(myID, newValue);
//	} else itemAmounts.put(myID, 1);
//}
//for(int i = 0; i < auctions.length; i++) {
//	newAuctionItems.add(auctions[i]);
//	String myID = getMyIDFromName(auctions[i].item_name);
//	float minPrice;
//	if(itemLowestBins.containsKey(myID))
//		minPrice = itemLowestBins.get(myID);
//	else minPrice = 0;
//	
//	if(auctions[i].starting_bid <= minPrice - minProfit) {
//		Risk risk;
//		int amount;
//		if(itemAmounts.containsKey(myID))
//			amount = itemAmounts.get(myID);
//		else
//			amount = 0;
//		if(amount < 5) 
//			risk = Risk.Insane;
//		else if(amount < 25)
//			risk = Risk.High;
//		else if (amount < 100) 
//			risk = Risk.Medium;
//		else 
//			risk = Risk.Low;
//		if(risk.ordinal() <= FruitBin.maxRisk.ordinal())
//			result.add(new AuctionInfo(auctions[i], auctions[i].starting_bid, itemLowestBins.get(myID), risk));
//	}
//	
//newAuctionItems.add(auctions[i]);
//}
//prevAuctions = newAuctionItems.toArray(new AuctionItem[result.size()]);
//return result.toArray(new AuctionInfo[result.size()]);
//}