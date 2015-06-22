package biz.no_ip.danie_dutoit.clashofclans;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Danie on 2015/06/06.
 */
public class StatsWarProgressGrid extends BaseAdapter {
    List<statWarProgressRecord> statWarProgressRecords;
    private Context mContext;

    public StatsWarProgressGrid(Context c, List<statWarProgressRecord> statWarProgressRecords) {
        mContext = c;
        this.statWarProgressRecords = statWarProgressRecords;
    }

    @Override
    public int getCount() {
        return statWarProgressRecords.size();
    }

    @Override
    public Object getItem(int position) {
        return statWarProgressRecords.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Integer i;
        String s;
        View grid;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            grid = inflater.inflate(R.layout.grid_war_progress_stats_single, parent, false);
        } else {
            grid = convertView;
        }

        TextView gn = (TextView) grid.findViewById(R.id.stat_gamename);
        s = statWarProgressRecords.get(position).gameName;
        gn.setText(s);
//
        TextView at = (TextView) grid.findViewById(R.id.stat_attackText);
        at.setText(statWarProgressRecords.get(position).attack);
//
        ImageView st = (ImageView) grid.findViewById(R.id.stat_starsTaken);
        switch (statWarProgressRecords.get(position).starsTaken) {
            case 0:
                st.setImageResource(R.drawable.flat_no_stars);
                break;
            case 1:
                st.setImageResource(R.drawable.flat_one_star);
                break;
            case 2:
                st.setImageResource(R.drawable.flat_two_stars);
                break;
            case 3:
                st.setImageResource(R.drawable.flat_three_stars);
                break;
            default:
                st.setImageResource(R.drawable.no_attack);
        }

//        i = statWarProgressRecords.get(position).ourRank;
//        s = String.valueOf(i);
//        TextView or = (TextView) grid.findViewById(R.id.stat_ourRank);
//        or.setText(s);
//
        i = statWarProgressRecords.get(position).theirRank;
        s = String.valueOf(i);
        TextView tr = (TextView) grid.findViewById(R.id.stat_theirRank);
        if (s.compareTo("-1") == 0) {
            tr.setText("None");
        } else {
            tr.setText(s);
        }
        return grid;
    }
}
