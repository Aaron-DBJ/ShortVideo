package com.arron_dbj.camera2demo.effect;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.arron_dbj.camera2demo.R;

/**
 * Created by aiya on 2017/7/27.
 */

public class ImageHolder extends RecyclerView.ViewHolder {

    public ImageView effect;

    public ImageHolder(View itemView, View.OnClickListener clickListener) {
        super(itemView);
        effect = (ImageView) itemView.findViewById(R.id.mImage);
        effect.setOnClickListener(clickListener);
    }

}
