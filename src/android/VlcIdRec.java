package itri.icl.k400.vlcid.libAns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by 940763 on 2017/5/4.
 */

public class VlcIdRec {

    HashMap<Integer, HashMap<String, Integer>> bitSizeMap = null;

    public VlcIdRec() {
        bitSizeMap = new HashMap<Integer, HashMap<String,Integer>>();
    }


    public void setVlcIdMap(int bitSize, HashMap<String, Integer> vlcIdMap){
        bitSizeMap.put(bitSize, vlcIdMap);
    }

    public HashMap<String, Integer> getVlcIdMap(int bitSize){
        return bitSizeMap.get(bitSize);

    }

    // 升序比较器
    private Comparator<Map.Entry<String, Integer>> valueComparator = new Comparator<Map.Entry<String,Integer>>() {
        @Override
        public int compare(Map.Entry<String, Integer> o1,
                           Map.Entry<String, Integer> o2) {
            return o2.getValue()-o1.getValue();
        }
    };


    private List<Map.Entry<String, Integer>> sortMap(HashMap<String, Integer> inputMap){
        // map转换成list进行排序
        List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String,Integer>>(inputMap.entrySet());
        // 排序
        Collections.sort(list,valueComparator);
        return list;
    }

    public VlcIdAns getBestVlcId(){
        int max_count = 0;
        int max_bitSize = 0;
        String max_vlcId = null;
        HashMap<String, Integer> bestVlcIdMap = null;
        List<Map.Entry<String, Integer>> ansList = null;

        assert(bitSizeMap!=null);
        for(Integer Key_bitSize: bitSizeMap.keySet()){
            HashMap<String, Integer> _vlcIdMap = bitSizeMap.get(Key_bitSize);
            if(_vlcIdMap != null){
                for(String Key_vlcId: _vlcIdMap.keySet()){
                    int count = _vlcIdMap.get(Key_vlcId);
                    if(count >= max_count){
                        if(Key_bitSize > max_bitSize ){
                            max_count = count;
                            max_bitSize = (int)Key_bitSize;
                            max_vlcId = Key_vlcId;
                            bestVlcIdMap = _vlcIdMap;
                        }
                    }
                }
            }
        }
        //get the MaxCount , sort the result according to it
        if(bestVlcIdMap != null){
            ansList = sortMap(bestVlcIdMap);
        }

        VlcIdAns myAns = new VlcIdAns(max_bitSize, ansList );
        return myAns;
    }
}

