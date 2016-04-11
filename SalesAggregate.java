package Aggregate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SalesAggregate {

	private static String branchListFileName = "branch.lst";
	private static String commodityListFileName = "commodity.lst";
	private static String branchOutPutFileName = "branch.out";
	private static String commodityOutPutFileName = "commodity.out";
	private static String branchRegexp = "^\\d{3}";
	private static String commodityRegexp = "^\\w{8}$";

	public static void main(String[] args) throws Exception{

		if(args.length != 1){
			System.out.println("正しくディレクトリを指定してください。");
			return;
		}

		Map<String, String> branchMap = new HashMap<>();
		Map<String, Long> branchSalesMap = new HashMap<>();
		try {
			branchMap = loadFile(args, branchListFileName, branchRegexp, branchSalesMap);
			if (branchMap == null) {
				System.out.println("支店定義ファイルのフォーマットが不正です");
				return;
			}
		} catch (FileNotFoundException e) {
			System.out.println("支店定義ファイルが存在しません");
			return;
		} catch (IOException e) {
			return;
		}

		Map<String, String> commodityMap = new HashMap<>();
		Map<String, Long> commoditySalesMap = new HashMap<>();
		try {
			commodityMap = loadFile(args, commodityListFileName, commodityRegexp, commoditySalesMap);
			if (commodityMap == null) {
				System.out.println("商品定義ファイルのフォーマットが不正です");
				return;
			}
		} catch (FileNotFoundException e) {
			System.out.println("商品定義ファイルが存在しません");
			return;
		} catch (IOException e) {
			return;
		}

		List<String> salseFileName = numberCheck(args);
		if(salseFileName == null) {

			System.out.println("売上ファイル名が連番になっていません");
			return;
		} else if (salseFileName.isEmpty()) {

			System.out.println("売上ファイルが存在しません");
			return;
		}

		List<String> message = new ArrayList<>();
		try {
			branchSalesMap = doAggregate(args, 0, "支店コード", salseFileName, message ,branchSalesMap);
			if (branchSalesMap == null) {
				System.out.println(message.get(0));
				return;
			}
		} catch (IOException e) {
			return;
		}

		try {
			commoditySalesMap = doAggregate(args, 1, "商品コード", salseFileName, message, commoditySalesMap);
			if (commoditySalesMap == null) {
				System.out.println(message.get(0));
				return;
			}
		} catch (IOException e) {
			return;
		}

		try{
			fileOutPut(args, branchOutPutFileName,  branchMap, branchSalesMap);
		}catch(IOException e){
			return;
		}

		try{
			fileOutPut(args, commodityOutPutFileName,  commodityMap, commoditySalesMap);
		}catch(IOException e){
			return;
		}
	}

	private static Map<String, String> loadFile(String[] args, String fileName, String regexp,
			Map<String, Long> commoditySalesMap)
			throws IOException {
		Map<String, String> ret = new HashMap<>();
		BufferedReader br = null;
		try {
			FileReader file = new FileReader(args[0] + File.separator + fileName);
			br = new BufferedReader(file);
			String str;
			while((str = br.readLine()) != null) {
				String[] format = str.split(",");
				if(format.length != 2){
					return null;
				}
				if(!format[0].matches(regexp)){
					return null;
				}
				ret.put(format[0], format[1]);
				commoditySalesMap.put(format[0], (long) 0);
			}
		} catch(FileNotFoundException e){
			throw e;
		}catch(IOException e){
			throw e;
		}finally{
			if (br != null){
				br.close();
			}
		}
		return ret;
	}

	private static List<String> numberCheck(String[] args) {
		List<String> ret = new ArrayList<>();
		File dir = new File(args[0]);
		File[] files = dir.listFiles();
		for(int i = 0; i < files.length; i++){
			if(files[i].getName().matches("^\\d{8}.rcd$") && files[i].isFile()){
				ret.add(files[i].getName());
			}
		}
		String[] rcd = new String[ret.size()];
		for(int i=0; i < ret.size(); i++){
			rcd = ret.get(i).split("\\.");
			Arrays.sort(rcd);
			if(i + 1 != Integer.parseInt(rcd[0])){
				ret = null;
				return ret;
			}
		}
		return ret;
	}

	private static Map<String, Long> doAggregate(
				String[] args,
				int call,
				String cordName,
				List<String> salseFileName,
				List<String> message,
				Map<String, Long> branchSalesMap
			) throws IOException {
		Map<String, Long> ret = branchSalesMap;
		BufferedReader br = null;
		try {
			for(int i = 0; i < salseFileName.size(); i++){
				List<String> salesRead = new ArrayList<String>();
				br = new BufferedReader(new FileReader(args[0] + File.separator + salseFileName.get(i)));
				String str;
				while((str = br.readLine()) != null){
					salesRead.add(str);
				}
				if (salesRead.isEmpty() || salesRead.size() != 3) {
					message.add(salseFileName.get(i) + "のフォーマットが不正です");
					ret = null;
					return ret;
				}
				if(ret.containsKey(salesRead.get(call))){
					ret.put(salesRead.get(call), ret.get(salesRead.get(call)) + Long.parseLong(salesRead.get(2)));

				} else {
					message.add(salseFileName.get(i) + "の" + cordName + "が不正です");

					ret = null;
					return ret;
				}
				for(String key : ret.keySet()){
					if(ret.get(key).toString().length() > 10){

						message.add("合計金額が10桁を超えています");
						return ret;
					}
				}
			}
		} catch(IOException e) {
			throw e;
		} finally {
			if(br != null) {
				br.close();
			}
		}
		return ret;
	}

	static void fileOutPut(
			String[] args,
			String fileName,
			Map<String, String> map,
			Map<String, Long> salesMap) throws IOException {

		FileWriter fw = null;
		try{
			File file = new File(args[0] + File.separator + fileName);
			fw = new FileWriter(file);
			List<Map.Entry<String,Long>> entries = new ArrayList<Map.Entry<String,Long>>(salesMap.entrySet());
				Collections.sort(entries, new Comparator<Map.Entry<String,Long>>() {
					@Override
					public int compare(Entry<String,Long> entry1, Entry<String,Long> entry2) {
							return ((Long)entry2.getValue()).compareTo((Long)entry1.getValue());
						}
					});
			for(Entry<String, Long> entry : entries){
				fw.write(entry.getKey() + "," + map.get(entry.getKey()) + "," +
						entry.getValue() + System.getProperty("line.separator"));
			}
		}
		catch(IOException e){
			throw e;
		}finally{
			if(fw != null){
				fw.close();
			}
		}

	}
}
