/*
 * Copyright (c) 2019. MotionEyeClient by Developer From Jokela, All Rights Reserved.
 * Licenced with MIT;
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.developerfromjokela.motioneyeclient.ui.fragments;


import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.developerfromjokela.motioneyeclient.R;
import com.developerfromjokela.motioneyeclient.classes.Device;
import com.developerfromjokela.motioneyeclient.database.Source;
import com.developerfromjokela.motioneyeclient.other.Utils;
import com.developerfromjokela.motioneyeclient.ui.activities.CameraViewer;
import com.developerfromjokela.motioneyeclient.ui.adapters.DevicesAdapter;
import com.developerfromjokela.motioneyeclient.ui.setup.activities.SetupStartScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class DevicesFragment extends android.support.v4.app.Fragment implements DevicesAdapter.DevicesAdapterListener {

    private RecyclerView camerasRecyclerView;
    private DevicesAdapter adapter;
    private Source database;
    private FloatingActionButton addCamera;
    private LinearLayout emptyView;
    private List<Device> deviceList = new ArrayList<>();


    public DevicesFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_cameras, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews(view);
    }

    @Override
    public void onDeviceClicked(int position, Device device) {
        Intent viewer = new Intent(getActivity(), CameraViewer.class);
        viewer.putExtra("DeviceId", device.getID());
        startActivity(viewer);
    }

    private void setListeners() {
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {

            @Override
            public void onChanged() {
                super.onChanged();
                checkEmpty();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                checkEmpty();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                checkEmpty();
            }


        });
        addCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().startActivity(new Intent(getActivity(), SetupStartScreen.class));
            }
        });
    }

    private void loadFromDatabase() {
        try {
            deviceList.clear();
            deviceList.addAll(database.getAll());
            for (Device device : deviceList) {
                adapter.notifyItemInserted(deviceList.indexOf(device));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        checkEmpty();

    }

    private void setupViews(View view) {
        emptyView = view.findViewById(R.id.emptyView);
        addCamera = view.findViewById(R.id.addItem);
        camerasRecyclerView = view.findViewById(R.id.camerasRecyclerView);
        camerasRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 1));
        camerasRecyclerView.addItemDecoration(new Utils.GridSpacingItemDecoration(1, Utils.dpToPx(getActivity()), true));

    }

    private void initializeObjects() {
        adapter = new DevicesAdapter(getActivity(), deviceList, this);
        camerasRecyclerView.setAdapter(adapter);
        database = new Source(getActivity());
    }

    private void checkEmpty() {
        emptyView.setVisibility(adapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);

        emptyView.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        initializeObjects();
        setListeners();
        loadFromDatabase();
    }


}