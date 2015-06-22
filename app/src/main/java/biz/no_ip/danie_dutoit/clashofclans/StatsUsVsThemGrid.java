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
public class StatsUsVsThemGrid extends BaseAdapter {
    List<statUsVsThemRecord> statUsVsThemRecords;
    private Context mContext;

    public StatsUsVsThemGrid(Context c, List<statUsVsThemRecord> statUsVsThemRecords) {
        mContext = c;
        this.statUsVsThemRecords = statUsVsThemRecords;
    }

    @Override
    public int getCount() {
        return statUsVsThemRecords.size();
    }

    @Override
    public Object getItem(int position) {
        return statUsVsThemRecords.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String i;
        String s;
        View grid;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            grid = inflater.inflate(R.layout.grid_us_vs_them_single, parent, false);
        } else {
            grid = convertView;
        }

        TextView gn = (TextView) grid.findViewById(R.id.stat_gamename);
        s = statUsVsThemRecords.get(position).gameName;
        gn.setText(s);

        i = statUsVsThemRecords.get(position).ourRank;
        s = String.valueOf(i);
        TextView or = (TextView) grid.findViewById(R.id.stat_ourRank);
        or.setText(s);

        i = statUsVsThemRecords.get(position).ourExperience;
        s = String.valueOf(i);
        TextView oe = (TextView) grid.findViewById(R.id.stat_ourExperience);
        oe.setText(s);

        i = statUsVsThemRecords.get(position).ourTownHall;
        s = String.valueOf(i);
        TextView oth = (TextView) grid.findViewById(R.id.stat_ourTownHall);
        oth.setText(s);

        i = statUsVsThemRecords.get(position).theirRank;
        s = String.valueOf(i);
        TextView tr = (TextView) grid.findViewById(R.id.stat_theirRank);
        tr.setText(s);

        i = statUsVsThemRecords.get(position).theirExperience;
        s = String.valueOf(i);
        TextView te = (TextView) grid.findViewById(R.id.stat_theirExperience);
        te.setText(s);

        i = statUsVsThemRecords.get(position).theirTownHall;
        s = String.valueOf(i);
        TextView tth = (TextView) grid.findViewById(R.id.stat_theirTownHall);
        tth.setText(s);

        return grid;
    }
}
