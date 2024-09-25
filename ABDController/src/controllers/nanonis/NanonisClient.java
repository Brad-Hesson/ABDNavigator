package controllers.nanonis;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

import controllers.nanonis.records.Direction;
import controllers.nanonis.records.ScanAction;
import controllers.nanonis.records.ScanBuffer;
import controllers.nanonis.records.ScanData;
import controllers.nanonis.records.ScanFrame;
import controllers.nanonis.records.ScanProps;
import controllers.nanonis.records.ScanSpeed;
import controllers.nanonis.records.SpeedSetting;

public class NanonisClient {
    private String host;
    private int port;
    private Socket clientSocket;
    private TypeWriter out;
    private TypeReader in;
    private ReentrantLock lock;

    public ScanFrameTTL scanFrame;

    public NanonisClient(String host, int port) throws UnknownHostException, IOException {
        super();
        this.host = host;
        this.port = port;
        clientSocket = new Socket(host, port);
        out = new TypeWriter(clientSocket.getOutputStream());
        in = new TypeReader(new DataInputStream(clientSocket.getInputStream()));
        lock = new ReentrantLock(true);
        scanFrame = new ScanFrameTTL(this);
    }

    public void close() throws IOException {
        lock.lock();
        clientSocket.close();
        lock.unlock();
    }

    private void tryReconnect() {
        int i = 0;
        while (true) {
            if (i > 0) {
                System.err.println("Attempting Nanonis reconnect...");
            }
            try {
                clientSocket = new Socket(host, port);
                out = new TypeWriter(clientSocket.getOutputStream());
                in = new TypeReader(new DataInputStream(clientSocket.getInputStream()));
                if (i > 0) {
                    System.err.println("Reconnect Successful");
                }
                return;
            } catch (IOException e) {
            }
            if (i == 0) {
                System.err.println("Nanonis disconnected");
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            i += 1;
        }
    }

    public float ZCtrlZPosGet() throws IOException, NanonisException, ResponseException {
        String name = "ZCtrl.ZPosGet";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 0);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                float z_pos = in.readFloat32();
                NanonisException.checkError(in.readError());
                lock.unlock();
                return z_pos;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public void FolMeXYPosSet(
            double xPosM,
            double yPosM,
            boolean blockOnMove)
            throws IOException, NanonisException, ResponseException {
        String name = "FolMe.XYPosSet";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 20);
                out.writeFloat64(xPosM);
                out.writeFloat64(yPosM);
                out.writeBool(blockOnMove);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                NanonisException.checkError(in.readError());
                lock.unlock();
                break;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public double[] FolMeXYPosGet(boolean waitNewestData)
            throws IOException, NanonisException, ResponseException {
        String name = "FolMe.XYPosGet";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 4);
                out.writeBool(waitNewestData);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                double x = in.readFloat64();
                double y = in.readFloat64();
                NanonisException.checkError(in.readError());
                lock.unlock();
                return new double[] { x, y };
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public void FolMeSpeedSet(float speedMps, boolean useCustomSpeed)
            throws IOException, NanonisException, ResponseException {
        String name = "FolMe.SpeedSet";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 8);
                out.writeFloat32(speedMps);
                out.writeBool(useCustomSpeed);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                NanonisException.checkError(in.readError());
                lock.unlock();
                break;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public SpeedSetting FolMeSpeedGet()
            throws IOException, NanonisException, ResponseException {
        String name = "FolMe.SpeedGet";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 0);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                SpeedSetting ret = new SpeedSetting(
                        in.readFloat32(),
                        in.readBool());
                NanonisException.checkError(in.readError());
                lock.unlock();
                return ret;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public float[] ScanXYPosGet(boolean waitNewestData)
            throws IOException, NanonisException, ResponseException {
        String name = "Scan.XYPosGet";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 4);
                out.writeBool(waitNewestData);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                float x = in.readFloat32();
                float y = in.readFloat32();
                NanonisException.checkError(in.readError());
                lock.unlock();
                return new float[] { x, y };
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public ScanData ScanFrameDataGrab(long channelIndex, boolean scanDirectionForward)
            throws IOException, NanonisException, ResponseException, UnsignedException {
        String name = "Scan.FrameDataGrab";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 8);
                out.writeUInt32(channelIndex);
                out.writeBool(scanDirectionForward);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                ScanData ret = new ScanData(
                        in.readString(),
                        in.readArrayFloat32_2D(),
                        in.readBool());
                NanonisException.checkError(in.readError());
                lock.unlock();
                return ret;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public void ZCtrlZPosSet(float zPosM)
            throws IOException, NanonisException, ResponseException {
        String name = "ZCtrl.ZPosSet";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 4);
                out.writeFloat32(zPosM);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                NanonisException.checkError(in.readError());
                lock.unlock();
                break;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public void BiasSet(float voltage)
            throws IOException, NanonisException, ResponseException {
        String name = "Bias.Set";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 4);
                out.writeFloat32(voltage);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                NanonisException.checkError(in.readError());
                lock.unlock();
                break;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public float BiasGet()
            throws IOException, NanonisException, ResponseException {
        String name = "Bias.Get";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 0);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                float voltage = in.readFloat32();
                NanonisException.checkError(in.readError());
                lock.unlock();
                return voltage;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public void ZCtrlSetpntSet(float setpoint)
            throws IOException, NanonisException, ResponseException {
        String name = "ZCtrl.SetpntSet";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 4);
                out.writeFloat32(setpoint);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                NanonisException.checkError(in.readError());
                lock.unlock();
                break;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public float ZCtrlSetpntGet()
            throws IOException, NanonisException, ResponseException {
        String name = "ZCtrl.SetpntGet";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 0);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                float setpoint = in.readFloat32();
                NanonisException.checkError(in.readError());
                lock.unlock();
                return setpoint;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public float CurrentGet()
            throws IOException, NanonisException, ResponseException {
        String name = "Current.Get";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 0);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                float current = in.readFloat32();
                NanonisException.checkError(in.readError());
                lock.unlock();
                return current;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public void ZCtrlOnOffSet(boolean state)
            throws IOException, NanonisException, ResponseException {
        String name = "ZCtrl.OnOffSet";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 4);
                out.writeBool(state);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                NanonisException.checkError(in.readError());
                lock.unlock();
                break;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public ScanFrame ScanFrameGet()
            throws IOException, NanonisException, ResponseException {
        String name = "Scan.FrameGet";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 0);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                ScanFrame ret = new ScanFrame(
                        in.readFloat32(),
                        in.readFloat32(),
                        in.readFloat32(),
                        in.readFloat32(),
                        in.readFloat32());
                NanonisException.checkError(in.readError());
                lock.unlock();
                return ret;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public void ScanFrameSet(float centerXM, float centerYM, float widthM, float heightM, float angleDeg)
            throws IOException, NanonisException, ResponseException {
        String name = "Scan.FrameSet";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 20);
                out.writeFloat32(centerXM);
                out.writeFloat32(centerYM);
                out.writeFloat32(widthM);
                out.writeFloat32(heightM);
                out.writeFloat32(angleDeg);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                NanonisException.checkError(in.readError());
                lock.unlock();
                break;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public ScanBuffer ScanBufferGet()
            throws IOException, NanonisException, ResponseException {
        String name = "Scan.BufferGet";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 0);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                ScanBuffer ret = new ScanBuffer(
                        in.readArrayInt1D(),
                        in.readInt(),
                        in.readInt());
                NanonisException.checkError(in.readError());
                lock.unlock();
                return ret;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public void ScanBufferSet(int[] channelIndexes, int pixelsPerLine, int linesPerScan)
            throws IOException, NanonisException, ResponseException {
        String name = "Scan.BufferSet";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 12 + channelIndexes.length * 4);
                out.writeArrayInt1D(channelIndexes);
                out.writeInt(pixelsPerLine);
                out.writeInt(linesPerScan);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                NanonisException.checkError(in.readError());
                lock.unlock();
                break;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public void ScanAction(ScanAction action, boolean scanDirectionUp)
            throws IOException, NanonisException, ResponseException {
        String name = "Scan.Action";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 6);
                try {
                    out.writeUInt16(action.toInt());
                } catch (UnsignedException e) {
                    e.printStackTrace();
                }
                out.writeBool(scanDirectionUp);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                NanonisException.checkError(in.readError());
                lock.unlock();
                break;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public boolean ScanStatusGet()
            throws IOException, NanonisException, ResponseException {
        String name = "Scan.StatusGet";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 0);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                boolean running = in.readBool();
                NanonisException.checkError(in.readError());
                lock.unlock();
                return running;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public void ZCtrlWithdraw(boolean waitUntilFinished, int timeoutMS)
            throws IOException, NanonisException, ResponseException {
        String name = "ZCtrl.Withdraw";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 8);
                out.writeBool(waitUntilFinished);
                out.writeInt(timeoutMS);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                NanonisException.checkError(in.readError());
                lock.unlock();
                break;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public void AutoApproachOpen()
            throws IOException, NanonisException, ResponseException {
        String name = "AutoApproach.Open";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 0);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                NanonisException.checkError(in.readError());
                lock.unlock();
                break;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public void AutoApproachOnOffSet(boolean state)
            throws IOException, NanonisException, ResponseException {
        String name = "AutoApproach.OnOffSet";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 2);
                try {
                    out.writeUInt16(state ? 1 : 0);
                } catch (UnsignedException e) {
                    e.printStackTrace();
                }
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                NanonisException.checkError(in.readError());
                lock.unlock();
                break;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public void MotorFreqAmpSet(float freqHz, float ampV, int axisSelector)
            throws IOException, NanonisException, ResponseException, UnsignedException {
        String name = "Motor.FreqAmpSet";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 10);
                out.writeFloat32(freqHz);
                out.writeFloat32(ampV);
                out.writeUInt16(axisSelector);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                NanonisException.checkError(in.readError());
                lock.unlock();
                break;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public void MotorStartMove(
            Direction direction,
            int numSteps,
            long group,
            boolean waitUntilFinished)
            throws IOException, NanonisException, ResponseException, UnsignedException {
        String name = "Motor.StartMove";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 14);
                out.writeUInt32(direction.toInt());
                out.writeUInt16(numSteps);
                out.writeUInt32(group);
                out.writeBool(waitUntilFinished);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                NanonisException.checkError(in.readError());
                lock.unlock();
                break;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public void ScanPropsSet(
            boolean continuousScan,
            boolean bouncyScan,
            boolean autoSave,
            String seriesName,
            String comment,
            String[] modulesNames)
            throws IOException, NanonisException, ResponseException {
        String name = "Scan.PropsSet";
        int len = 12 +
                seriesName.getBytes().length + 4 +
                comment.getBytes().length + 4 +
                8;
        for (String mn : modulesNames) {
            len += mn.getBytes().length + 4;
        }
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, len);
                try {
                    out.writeUInt32(continuousScan ? 1 : 2);
                    out.writeUInt32(bouncyScan ? 1 : 2);
                    out.writeUInt32(autoSave ? 1 : 3);
                } catch (UnsignedException e) {
                    e.printStackTrace();
                }
                out.writeString(seriesName);
                out.writeString(comment);
                out.writeArrayString1D(modulesNames);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                NanonisException.checkError(in.readError());
                lock.unlock();
                break;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public float[] PiezoRangeGet()
            throws IOException, NanonisException, ResponseException {
        String name = "Piezo.RangeGet";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 0);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                float[] ranges = new float[] {
                        in.readFloat32(),
                        in.readFloat32(),
                        in.readFloat32(),
                };
                NanonisException.checkError(in.readError());
                lock.unlock();
                return ranges;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public void ScanSpeedSet(float fSpeedMps,
            float bSpeedMps,
            float fLineTimeS,
            float bLineTimeS,
            int whichParamConst,
            float fbRatio)
            throws IOException, NanonisException, ResponseException, UnsignedException {
        String name = "Scan.SpeedSet";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 22);
                out.writeFloat32(fSpeedMps);
                out.writeFloat32(bSpeedMps);
                out.writeFloat32(fLineTimeS);
                out.writeFloat32(bLineTimeS);
                out.writeUInt16(whichParamConst);
                out.writeFloat32(fbRatio);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                NanonisException.checkError(in.readError());
                lock.unlock();
                return;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public ScanSpeed ScanSpeedGet()
            throws IOException, NanonisException, ResponseException {
        String name = "Scan.SpeedGet";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 0);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                ScanSpeed scanSpeed = new ScanSpeed(
                        in.readFloat32(),
                        in.readFloat32(),
                        in.readFloat32(),
                        in.readFloat32(),
                        in.readUInt16(),
                        in.readFloat32());
                NanonisException.checkError(in.readError());
                lock.unlock();
                return scanSpeed;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public float[] MotorFreqAmpGet(int axisSelector)
            throws IOException, NanonisException, ResponseException, UnsignedException {
        String name = "Motor.FreqAmpGet";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 2);
                out.writeUInt16(axisSelector);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                float[] freqAmp = new float[] {
                        in.readFloat32(),
                        in.readFloat32()
                };
                NanonisException.checkError(in.readError());
                lock.unlock();
                return freqAmp;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public ScanProps ScanPropsGet()
            throws IOException, NanonisException, ResponseException, UnsignedException {
        String name = "Scan.PropsGet";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 0);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                ScanProps props = new ScanProps(
                        in.readUInt32() != 0,
                        in.readUInt32() != 0,
                        in.readUInt32() != 2,
                        in.readString(),
                        in.readString());
                NanonisException.checkError(in.readError());
                lock.unlock();
                return props;
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    public void TipShaperStart(boolean wait_until_finished, int timeout)
            throws IOException, NanonisException, ResponseException, UnsignedException {
        String name = "TipShaper.Start";
        lock.lock();
        while (true) {
            try {
                out.writeHeader(name, 8);
                out.writeBool(wait_until_finished);
                out.writeInt(timeout);
                out.flush();
                ResponseException.checkHeader(name, in.readHeader());
                NanonisException.checkError(in.readError());
                lock.unlock();
            } catch (IOException e) {
                tryReconnect();
            }
        }
    }

    private void test() {

        try {
            float v = ZCtrlZPosGet();
            System.out.println("ZCtrlZPosGet: " + v);

            FolMeXYPosSet(12.34, 56.78, true);
            System.out.println("FolMeXYPosSet: ");

            double[] xy = FolMeXYPosGet(true);
            System.out.println("FolMeXYPosGet: " + Arrays.toString(xy));

            FolMeSpeedSet(12.34f, true);
            System.out.println("FolMeSpeedSet: ");

            SpeedSetting speed = FolMeSpeedGet();
            System.out.println("FolMeSpeedGet: " + speed);

            float[] xy2 = ScanXYPosGet(true);
            System.out.println("ScanXYPosGet: " + Arrays.toString(xy2));

            ScanData scan_data = ScanFrameDataGrab(0, true);
            System.out.println("ScanFrameDataGrab: " + scan_data);

            ZCtrlZPosSet(12.34f);
            System.out.println("ZCtrlZPosSet: ");

            BiasSet(12.34f);
            System.out.println("BiasSet: ");

            float voltage = BiasGet();
            System.out.println("BiasGet: " + voltage);

            ZCtrlSetpntSet(12.34f);
            System.out.println("ZCtrlSetpntSet: ");

            float setpoint = ZCtrlSetpntGet();
            System.out.println("ZCtrlSetpntGet: " + setpoint);

            float current = CurrentGet();
            System.out.println("CurrentGet: " + current);

            ZCtrlOnOffSet(true);
            System.out.println("ZCtrlOnOffSet: ");

            ScanFrame scanFrame = ScanFrameGet();
            System.out.println("ScanFrameGet: " + scanFrame);

            ScanFrameSet(0.1f, 0.2f, 1.1f, 1.2f, 3.14f);
            System.out.println("ScanFrameSet: ");

            ScanBuffer scanBuffer = ScanBufferGet();
            System.out.println("ScanBufferGet: " + scanBuffer);

            ScanBufferSet(new int[] { 1, 2, 3 }, 1, 2);
            System.out.println("ScanBufferSet: ");

            ScanAction(ScanAction.FREEZE, true);
            System.out.println("ScanAction: ");

            boolean running = ScanStatusGet();
            System.out.println("ScanStatusGet: " + running);

            ZCtrlWithdraw(true, -1);
            System.out.println("ZCtrlWithdraw: ");

            AutoApproachOpen();
            System.out.println("AutoApproachOpen: ");

            AutoApproachOnOffSet(true);
            System.out.println("AutoApproachOnOffSet: ");

            MotorFreqAmpSet(1.2f, 2.3f, 4);
            System.out.println("MotorFreqAmpSet: ");

            MotorStartMove(Direction.Y_POS, 5, 0, true);
            System.out.println("MotorStartMove: ");

            ScanPropsSet(false, false, false, "series", "comment", new String[] { "mod1", "mod2" });
            System.out.println("ScanPropsSet: ");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NanonisException e) {
            e.printStackTrace();
        } catch (ResponseException e) {
            e.printStackTrace();
        } catch (UnsignedException e) {
            e.printStackTrace();
        }
    }
}