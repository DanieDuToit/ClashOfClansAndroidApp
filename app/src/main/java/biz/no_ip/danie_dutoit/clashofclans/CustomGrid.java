package biz.no_ip.danie_dutoit.clashofclans;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by Danie on 2015/06/06.
 */
public class CustomGrid extends BaseAdapter {
    private Context mContext;
    private List<String> web;
    private List<Integer> Imageid;
    private int numberOfParticipants;
    private HashMap<Integer, Integer> attacks;

    public CustomGrid(Context c, List<String> web, List<Integer> Imageid, int numberOfParticipants, HashMap<Integer, Integer> attacks) {
        this.attacks = attacks;
        this.numberOfParticipants = numberOfParticipants;
        mContext = c;
        this.Imageid = Imageid;
        this.web = web;
    }

    @Override
    public int getCount() {
        return numberOfParticipants;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View grid;
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);


//        grid = inflater.inflate(R.layout.grid_single, null);
//        TextView textView = (TextView) grid.findViewById(R.id.rank_text);
//        ImageView imageView = (ImageView) grid.findViewById(R.id.grid_image);
//        textView.setText(web[position]);
//        imageView.setImageResource(R.drawable.stars_none);

        if (convertView == null) {
            grid = inflater.inflate(R.layout.grid_single, null);

            TextView textView = (TextView) grid.findViewById(R.id.rank_text);
            textView.setWidth(112);
            textView.setGravity(View.TEXT_ALIGNMENT_CENTER);
            textView.setText(web.get( position));

            ImageView imageView = (ImageView) grid.findViewById(R.id.grid_image);
            imageView.setImageResource(Imageid.get(position));

        } else {
            grid = (View) convertView;
        }

        return grid;
    }
}
