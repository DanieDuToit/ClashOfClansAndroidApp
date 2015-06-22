package biz.no_ip.danie_dutoit.clashofclans;

import android.content.Context;
import android.media.Image;
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
public class StarsLeftToBeWinGrid extends BaseAdapter {
    List<starsLeftToBeWinRecord> starsLeftToBeWinRecords;
    private Context mContext;

    public StarsLeftToBeWinGrid(Context c, List<starsLeftToBeWinRecord> starsLeftToBeWinRecords) {
        mContext = c;
        this.starsLeftToBeWinRecords = starsLeftToBeWinRecords;
    }

    @Override
    public int getCount() {
        return starsLeftToBeWinRecords.size();
    }

    @Override
    public Object getItem(int position) {
        return starsLeftToBeWinRecords.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView img;
        Integer i;
        String s;
        View grid;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            grid = inflater.inflate(R.layout.grid_stars_left_to_win_single, parent, false);
        } else {
            grid = convertView;
        }

        TextView gn = (TextView) grid.findViewById(R.id.stat_theirRank);
        s = starsLeftToBeWinRecords.get(position).theirRank;
        gn.setText(s);

        i = starsLeftToBeWinRecords.get(position).starsLeftImage;
        ImageView or = (ImageView) grid.findViewById(R.id.stars_left_to_be_win_image);
        or.setImageResource(i);
        return grid;
    }
}
