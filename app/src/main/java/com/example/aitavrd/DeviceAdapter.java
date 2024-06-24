package com.example.aitavrd;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {
    private List<Device> deviceList;

    public DeviceAdapter(List<Device> deviceList) {
        this.deviceList = deviceList;
    }

    public interface OnDeviceClickListener {
        void onDeviceClick(Device device);
    }

    private OnDeviceClickListener listener;

    public DeviceAdapter(List<Device> deviceList, OnDeviceClickListener listener) {
        this.deviceList = deviceList;
        this.listener = listener;
    }



    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.device_list_item, parent, false);
        return new DeviceViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = deviceList.get(position);
        String deviceName = device.getName();
        Integer batteryLevel = device.getBatteryLevel();

        // Check for null deviceName and handle it appropriately
        if (deviceName != null) {
            holder.deviceName.setText(deviceName);
        } else {
            holder.deviceName.setText("Unknown Device");
        }

        if (batteryLevel != null && batteryLevel != -1) {
            holder.batteryLevel.setText(batteryLevel + "%");
        } else {
            holder.batteryLevel.setText(" ");
        }

        if (device.getPaired()) {
            holder.pairedStatusIcon.setImageResource(R.drawable.ic_ble_connected);
        } else {
            holder.pairedStatusIcon.setImageResource(R.drawable.ic_ble_disconnected);
        }

        holder.itemView.setOnClickListener(view -> {
            Device clickedDevice = deviceList.get(holder.getAdapterPosition());
            if (listener != null) {
                listener.onDeviceClick(clickedDevice);
            }
            Toast.makeText(view.getContext(), "Clicked: " + clickedDevice.getName(), Toast.LENGTH_SHORT).show();
        });
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        ImageView pairedStatusIcon;
        TextView deviceName;
        TextView batteryLevel;

        DeviceViewHolder(View view) {
            super(view);
            pairedStatusIcon = view.findViewById(R.id.imageViewPairedStatus);
            deviceName = view.findViewById(R.id.textViewDeviceName);
            batteryLevel = view.findViewById(R.id.textViewBatteryCharge);
        }
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

}