package itri.icl.k400.vlcid.libAns;

import java.util.List;
import java.util.Map;

/**
 * Created by 940763 on 2017/5/4.
 */

public class VlcIdAns {
    int bitSize;
    List<Map.Entry<String, Integer>> vlcIdList;


    public VlcIdAns(int bitSize, List<Map.Entry<String, Integer>> vlcIdList) {
        this.bitSize = bitSize;
        this.vlcIdList = vlcIdList;
    }

    public int getBestBitSize()
    {
        return bitSize;
    }

    public String getBestVlcIdStr()
    {
        String vlcIdStr = null;
        if(vlcIdList != null){
            vlcIdStr = vlcIdList.get(0).getKey();
        }
        return vlcIdStr;
    }

    public int getBestVlcIdCount()
    {
        int vlcIdCount = 0;
        if(vlcIdList != null){
            vlcIdCount = vlcIdList.get(0).getValue();
        }
        return vlcIdCount;
    }
}
