package mike.gamelogic;

import java.util.ArrayList;

/**
 * Created by mike on 10/5/2017.
 */

public class DataParser {
    public static ArrayList<DataMapping> parseIncomingData(String dataString){
        ArrayList<DataMapping> list = new ArrayList<>();
        int i = 0;
        int j = i; //keeps track of the old index
        String a = null;
        while((i = dataString.indexOf(':', i)) != -1){
            if(a != null){
                list.add(new DataMapping(a, dataString.substring(j, i)));
                a = null;
            }
            else{
                a = dataString.substring(j, i);
            }
            i++;
            j = i;
        }
        if(a != null && dataString.length() >= i){
            list.add(new DataMapping(a, dataString.substring(j)));
        }
        return list;
    }

    //a container. Not intended to be set apart from the constructor, though this is not enforced.
    public static class DataMapping{
        public String keyWord;
        public String value;
        DataMapping(String keyword, String value){
            this.keyWord = keyword;
            this.value = value;
        }
    }

    //returns the first int value found in the string, and Integer.MIN_VALUE if no int can be found
    public static int extractInt(String str){
        String s = "";
        for(int i = 0; i < str.length(); i++){
            if(str.charAt(i) >= '0' && str.charAt(i) <= '9'){
                s+=str.charAt(i);
            }
            else if(!s.equals(""))
                break;
        }
        if(s.equals("")) {
            return Integer.MIN_VALUE;
        }
        return Integer.parseInt(s);
    }
}
