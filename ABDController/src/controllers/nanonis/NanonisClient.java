package controllers.nanonis;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import controllers.nanonis.records.Direction;
import controllers.nanonis.records.ScanAction;
import controllers.nanonis.records.ScanBuffer;
import controllers.nanonis.records.ScanData;
import controllers.nanonis.records.ScanFrame;
import controllers.nanonis.records.ScanProps;
import controllers.nanonis.records.ScanSpeed;
import controllers.nanonis.records.SpeedSetting;
import controllers.nanonis.records.TipShaperProps;

public class NanonisClient {
    private static boolean LOG = true;
    private Socket clientSocket;
    private TypeWriter out;
    private TypeReader in;

    public NanonisClient(String host, int port) throws UnknownHostException, IOException {
        super();
        clientSocket = new Socket(host, port);
        out = new TypeWriter(clientSocket.getOutputStream());
        in = new TypeReader(new DataInputStream(clientSocket.getInputStream()));
    }

    private void log(String m) {
        if (LOG)
            System.out.println("[NANONIS CLIENT] " + m);
    }

    synchronized public void close() throws IOException {
        clientSocket.close();
    }

    synchronized public float ZCtrlZPosGet() throws IOException, NanonisException, ResponseException {
        log("ZCtrl.ZPosGet");
        String name = "ZCtrl.ZPosGet";
        out.writeHeader(name, 0);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        float z_pos = in.readFloat32();
        NanonisException.checkError(in.readError());
        return z_pos;

    }

    synchronized public void FolMeXYPosSet(
            double xPosM,
            double yPosM,
            boolean blockOnMove)
            throws IOException, NanonisException, ResponseException {
        log("FolMe.XYPosSet");
        String name = "FolMe.XYPosSet";
        out.writeHeader(name, 20);
        out.writeFloat64(xPosM);
        out.writeFloat64(yPosM);
        out.writeBool(blockOnMove);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        NanonisException.checkError(in.readError());

    }

    synchronized public double[] FolMeXYPosGet(boolean waitNewestData)
            throws IOException, NanonisException, ResponseException {
        log("FolMe.XYPosGet");
        String name = "FolMe.XYPosGet";
        out.writeHeader(name, 4);
        out.writeBool(waitNewestData);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        double x = in.readFloat64();
        double y = in.readFloat64();
        NanonisException.checkError(in.readError());
        return new double[] { x, y };

    }

    synchronized public void FolMeSpeedSet(float speedMps, boolean useCustomSpeed)
            throws IOException, NanonisException, ResponseException {
        log("FolMe.SpeedSet");
        String name = "FolMe.SpeedSet";
        out.writeHeader(name, 8);
        out.writeFloat32(speedMps);
        out.writeBool(useCustomSpeed);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        NanonisException.checkError(in.readError());

    }

    synchronized public SpeedSetting FolMeSpeedGet()
            throws IOException, NanonisException, ResponseException {
        log("FolMe.SpeedGet");
        String name = "FolMe.SpeedGet";
        out.writeHeader(name, 0);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        SpeedSetting ret = new SpeedSetting(
                in.readFloat32(),
                in.readBool());
        NanonisException.checkError(in.readError());
        return ret;

    }

    synchronized public float[] ScanXYPosGet(boolean waitNewestData)
            throws IOException, NanonisException, ResponseException {
        log("Scan.XYPosGet");
        String name = "Scan.XYPosGet";
        out.writeHeader(name, 4);
        out.writeBool(waitNewestData);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        float x = in.readFloat32();
        float y = in.readFloat32();
        NanonisException.checkError(in.readError());
        return new float[] { x, y };

    }

    synchronized public ScanData ScanFrameDataGrab(long channelIndex, boolean scanDirectionForward)
            throws IOException, NanonisException, ResponseException, UnsignedException {
        log("Scan.FrameDataGrab");
        String name = "Scan.FrameDataGrab";
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
        return ret;

    }

    synchronized public void ZCtrlZPosSet(float zPosM)
            throws IOException, NanonisException, ResponseException {
        log("ZCtrl.ZPosSet");
        String name = "ZCtrl.ZPosSet";
        out.writeHeader(name, 4);
        out.writeFloat32(zPosM);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        NanonisException.checkError(in.readError());

    }

    synchronized public void BiasSet(float voltage)
            throws IOException, NanonisException, ResponseException {
        log("Bias.Set");
        String name = "Bias.Set";
        out.writeHeader(name, 4);
        out.writeFloat32(voltage);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        NanonisException.checkError(in.readError());

    }

    synchronized public float BiasGet()
            throws IOException, NanonisException, ResponseException {
        String name = "Bias.Get";
        out.writeHeader(name, 0);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        float voltage = in.readFloat32();
        NanonisException.checkError(in.readError());
        return voltage;

    }

    synchronized public void ZCtrlSetpntSet(float setpoint)
            throws IOException, NanonisException, ResponseException {
        log("ZCtrl.SetpntSet");
        String name = "ZCtrl.SetpntSet";
        out.writeHeader(name, 4);
        out.writeFloat32(setpoint);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        NanonisException.checkError(in.readError());

    }

    synchronized public float ZCtrlSetpntGet()
            throws IOException, NanonisException, ResponseException {
        log("ZCtrl.SetpntGet");
        String name = "ZCtrl.SetpntGet";
        out.writeHeader(name, 0);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        float setpoint = in.readFloat32();
        NanonisException.checkError(in.readError());
        return setpoint;

    }

    synchronized public float CurrentGet()
            throws IOException, NanonisException, ResponseException {
        log("Current.Get");
        String name = "Current.Get";
        out.writeHeader(name, 0);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        float current = in.readFloat32();
        NanonisException.checkError(in.readError());
        return current;

    }

    synchronized public void ZCtrlOnOffSet(boolean state)
            throws IOException, NanonisException, ResponseException {
        log("ZCtrl.OnOffSet");
        String name = "ZCtrl.OnOffSet";
        out.writeHeader(name, 4);
        out.writeBool(state);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        NanonisException.checkError(in.readError());

    }

    synchronized public ScanFrame ScanFrameGet()
            throws IOException, NanonisException, ResponseException {
        log("Scan.FrameGet");
        String name = "Scan.FrameGet";
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
        return ret;

    }

    synchronized public void ScanFrameSet(float centerXM, float centerYM, float widthM, float heightM, float angleDeg)
            throws IOException, NanonisException, ResponseException {
        log("Scan.FrameSet");
        String name = "Scan.FrameSet";
        out.writeHeader(name, 20);
        out.writeFloat32(centerXM);
        out.writeFloat32(centerYM);
        out.writeFloat32(widthM);
        out.writeFloat32(heightM);
        out.writeFloat32(angleDeg);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        NanonisException.checkError(in.readError());

    }

    synchronized public ScanBuffer ScanBufferGet()
            throws IOException, NanonisException, ResponseException {
        log("Scan.BufferGet");
        String name = "Scan.BufferGet";
        out.writeHeader(name, 0);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        ScanBuffer ret = new ScanBuffer(
                in.readArrayInt1D(),
                in.readInt(),
                in.readInt());
        NanonisException.checkError(in.readError());
        return ret;

    }

    synchronized public void ScanBufferSet(int[] channelIndexes, int pixelsPerLine, int linesPerScan)
            throws IOException, NanonisException, ResponseException {
        log("Scan.BufferSet");
        String name = "Scan.BufferSet";
        out.writeHeader(name, 12 + channelIndexes.length * 4);
        out.writeArrayInt1D(channelIndexes);
        out.writeInt(pixelsPerLine);
        out.writeInt(linesPerScan);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        NanonisException.checkError(in.readError());

    }

    synchronized public void ScanAction(ScanAction action, boolean scanDirectionUp)
            throws IOException, NanonisException, ResponseException {
        log("Scan.Action");
        String name = "Scan.Action";
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

    }

    synchronized public boolean ScanStatusGet()
            throws IOException, NanonisException, ResponseException {
        log("Scan.StatusGet");
        String name = "Scan.StatusGet";
        out.writeHeader(name, 0);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        boolean running = in.readBool();
        NanonisException.checkError(in.readError());
        return running;

    }

    synchronized public void ZCtrlWithdraw(boolean waitUntilFinished, int timeoutMS)
            throws IOException, NanonisException, ResponseException {
        log("ZCtrl.Withdraw");
        String name = "ZCtrl.Withdraw";
        out.writeHeader(name, 8);
        out.writeBool(waitUntilFinished);
        out.writeInt(timeoutMS);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        NanonisException.checkError(in.readError());

    }

    synchronized public void AutoApproachOpen()
            throws IOException, NanonisException, ResponseException {
        log("AutoApproach.Open");
        String name = "AutoApproach.Open";
        out.writeHeader(name, 0);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        NanonisException.checkError(in.readError());

    }

    synchronized public void AutoApproachOnOffSet(boolean state)
            throws IOException, NanonisException, ResponseException {
        log("AutoApproach.OnOffSet");
        String name = "AutoApproach.OnOffSet";
        out.writeHeader(name, 2);
        try {
            out.writeUInt16(state ? 1 : 0);
        } catch (UnsignedException e) {
            e.printStackTrace();
        }
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        NanonisException.checkError(in.readError());

    }

    synchronized public void MotorFreqAmpSet(float freqHz, float ampV, int axisSelector)
            throws IOException, NanonisException, ResponseException, UnsignedException {
        log("Motor.FreqAmpSet");
        String name = "Motor.FreqAmpSet";
        out.writeHeader(name, 10);
        out.writeFloat32(freqHz);
        out.writeFloat32(ampV);
        out.writeUInt16(axisSelector);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        NanonisException.checkError(in.readError());

    }

    synchronized public void MotorStartMove(
            Direction direction,
            int numSteps,
            long group,
            boolean waitUntilFinished)
            throws IOException, NanonisException, ResponseException, UnsignedException {
        log("Motor.StartMove");
        String name = "Motor.StartMove";
        out.writeHeader(name, 14);
        out.writeUInt32(direction.toInt());
        out.writeUInt16(numSteps);
        out.writeUInt32(group);
        out.writeBool(waitUntilFinished);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        NanonisException.checkError(in.readError());

    }

    synchronized public void ScanPropsSet(
            boolean continuousScan,
            boolean bouncyScan,
            boolean autoSave,
            String seriesName,
            String comment,
            String[] modulesNames)
            throws IOException, NanonisException, ResponseException {
        log("Scan.PropsSet");
        String name = "Scan.PropsSet";
        int len = 12 +
                seriesName.getBytes().length + 4 +
                comment.getBytes().length + 4 +
                8;
        for (String mn : modulesNames) {
            len += mn.getBytes().length + 4;
        }
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

    }

    synchronized public float[] PiezoRangeGet()
            throws IOException, NanonisException, ResponseException {
        log("Piezo.RangeGet");
        String name = "Piezo.RangeGet";
        out.writeHeader(name, 0);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        float[] ranges = new float[] {
                in.readFloat32(),
                in.readFloat32(),
                in.readFloat32(),
        };
        NanonisException.checkError(in.readError());
        return ranges;

    }

    synchronized public void ScanSpeedSet(float fSpeedMps,
            float bSpeedMps,
            float fLineTimeS,
            float bLineTimeS,
            int whichParamConst,
            float fbRatio)
            throws IOException, NanonisException, ResponseException, UnsignedException {
        log("Scan.SpeedSet");
        String name = "Scan.SpeedSet";
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
        return;

    }

    synchronized public ScanSpeed ScanSpeedGet()
            throws IOException, NanonisException, ResponseException {
        log("Scan.SpeedGet");
        String name = "Scan.SpeedGet";
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
        return scanSpeed;

    }

    synchronized public float[] MotorFreqAmpGet(int axisSelector)
            throws IOException, NanonisException, ResponseException, UnsignedException {
        log("Motor.FreqAmpGet");
        String name = "Motor.FreqAmpGet";
        out.writeHeader(name, 2);
        out.writeUInt16(axisSelector);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        float[] freqAmp = new float[] {
                in.readFloat32(),
                in.readFloat32()
        };
        NanonisException.checkError(in.readError());
        return freqAmp;

    }

    synchronized public ScanProps ScanPropsGet()
            throws IOException, NanonisException, ResponseException, UnsignedException {
        log("Scan.PropsGet");
        String name = "Scan.PropsGet";
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
        return props;

    }

    synchronized public void TipShaperStart(boolean wait_until_finished, int timeout)
            throws IOException, NanonisException, ResponseException, UnsignedException {
        log("TipShaper.Start");
        String name = "TipShaper.Start";
        out.writeHeader(name, 8);
        out.writeBool(wait_until_finished);
        out.writeInt(timeout);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        NanonisException.checkError(in.readError());
    }

    synchronized public void BiasPulse(
            boolean wait_until_finished,
            float pulseTime,
            float pulseVoltage,
            int zCtrlHold,
            int absRel)
            throws IOException, NanonisException, ResponseException, UnsignedException {
        log("Bias.Pulse");
        String name = "Bias.Pulse";
        out.writeHeader(name, 16);
        out.writeBool(wait_until_finished);
        out.writeFloat32(pulseTime);
        out.writeFloat32(pulseVoltage);
        out.writeUInt16(zCtrlHold);
        out.writeUInt16(absRel);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        NanonisException.checkError(in.readError());
    }

    synchronized public TipShaperProps TipShaperPropsGet()
            throws IOException, NanonisException, ResponseException, UnsignedException {
        log("TipShaper.PropsGet");
        String name = "TipShaper.Start";
        out.writeHeader(name, 0);
        out.flush();
        ResponseException.checkHeader(name, in.readHeader());
        TipShaperProps props = new TipShaperProps(
                in.readFloat32(),
                in.readBool(),
                in.readFloat32(),
                in.readFloat32(),
                in.readFloat32(),
                in.readFloat32(),
                in.readFloat32(),
                in.readFloat32(),
                in.readFloat32(),
                in.readFloat32(),
                in.readBool());
        NanonisException.checkError(in.readError());
        return props;
    }
}