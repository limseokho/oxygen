import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.oxygen.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 10;
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> devices;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket = null;
    private OutputStream outputStream = null;
    private InputStream inputStream = null;
    private Thread workerThread = null;
    private byte[] readBuffer;
    private int readBufferPosition;
    private TextView oxygenSaturationTextView;
    private TextView heartRateTextView;
    String[] array = {"0"};
    int pairedDeviceCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        oxygenSaturationTextView = findViewById(R.id.oxygenSaturationTextView);
        heartRateTextView = findViewById(R.id.heartRateTextView);

        String[] permission_list = {
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
        };
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth 미지원 기기입니다.", Toast.LENGTH_SHORT).show();
        } else {
            if (bluetoothAdapter.isEnabled()) {
                selectBluetoothDevice();
            } else {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BT);
                selectBluetoothDevice();
            }
        }
    }

    public void receiveDate() {
        final Handler handler = new Handler();
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        int byteAvailable = inputStream.available();
                        if (byteAvailable > 0) {
                            byte[] bytes = new byte[byteAvailable];
                            inputStream.read(bytes);
                            for (int i = 0; i < byteAvailable; i++) {
                                byte tempByte = bytes[i];
                                if (tempByte == '\n') {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String text = new String(encodedBytes, "UTF-8");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {

                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = tempByte;
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        workerThread.start();
    }

    public void selectBluetoothDevice() {
        devices = bluetoothAdapter.getBondedDevices();
        pairedDeviceCount = devices.size();
        if (pairedDeviceCount == 0) {
            Toast.makeText(getApplicationContext(), "먼저 Bluetooth 설정에 들어가 페어링을 진행해 주세요.", Toast.LENGTH_SHORT).show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("페어링 된 블루투스 디바이스 목록");

            List<String> list = new ArrayList<>();
            for (BluetoothDevice bluetoothDevice : devices) {
                list.add(bluetoothDevice.getName());
            }
            list.add("취소");
            final CharSequence[] charSequences = list.toArray(new CharSequence[list.size()]);
            list.toArray(new CharSequence[list.size()]);
            builder.setItems(charSequences, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    connectDevice(charSequences[which].toString());
                }

            });
            builder.setCancelable(false);
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }

    public void connectDevice(String deviceName) {
        for (BluetoothDevice tempDevice : devices) {
            if (deviceName.equals(tempDevice.getName())) {
                bluetoothDevice = tempDevice;
                break;
            }
            Toast.makeText(getApplicationContext(), bluetoothDevice.getName() + "연결 완료!", Toast.LENGTH_SHORT).show();
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            try {
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                bluetoothSocket.connect();

                outputStream = bluetoothSocket.getOutputStream();
                inputStream = bluetoothSocket.getInputStream();
                receiveDate();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
