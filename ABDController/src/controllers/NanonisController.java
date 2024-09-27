package controllers;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Optional;

import com.ABDReverseClient;

import controllers.nanonis.NanonisClient;
import controllers.nanonis.NanonisClientPool;
import controllers.nanonis.NanonisException;
import controllers.nanonis.ResponseException;
import controllers.nanonis.ScanFrameTTL;
import controllers.nanonis.UnsignedException;
import controllers.nanonis.records.Direction;
import controllers.nanonis.records.ScanAction;
import controllers.nanonis.records.ScanBuffer;
import controllers.nanonis.records.ScanData;
import controllers.nanonis.records.ScanProps;
import controllers.nanonis.records.ScanSpeed;
import main.ABDController;
import main.ABDControllerInterface;
import main.BiasSignal;
import main.CurrentSignal;

public class NanonisController implements ABDControllerInterface {

    private NanonisClientPool clients;
    private ScanFrameTTL scanFrame;

    private BiasSignal biasSignal;
    private CurrentSignal currentSignal;

    private static boolean log = true;

    private int courseGroup = 0;

    int covLen;
    private HashMap<String, Integer> cov;;

    public NanonisController() {
        super();

        try {
            clients = new NanonisClientPool();
        } catch (IOException e) {
            e.printStackTrace();
        }
        scanFrame = new ScanFrameTTL(clients);

        biasSignal = new BiasSignal(this);
        biasSignal.units = "V";
        biasSignal.stepSize = 0.2; // V
        biasSignal.stepTime = 1; // ms

        currentSignal = new CurrentSignal(this);
        currentSignal.units = "nA";
        currentSignal.stepSize = 1; // nA
        currentSignal.stepTime = 1; // ms

        cov = new HashMap<String, Integer>(100);
        try {
            Method[] methods = Class.forName("main.ABDControllerInterface").getDeclaredMethods();
            for (Method m : methods) {
                String name = m.getName();
                log(name);
                cov.put(name, 0);
            }
            covLen = cov.size();
        } catch (SecurityException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void log(String message) {
        if (log) {
            System.out.println("[NANONIS] " + message);
        }
    }

    private void updateCov(String name) {
        int n = cov.get(name);
        cov.put(name, n + 1);
        int newCov = 0;
        for (int v : cov.values()) {
            newCov += (v == 0) ? 1 : 0;
        }
        if (newCov != covLen) {
            log("New coverage: " + name);
            // String s = "";
            // for (Entry<String, Integer> entry : cov.entrySet()) {
            // if (entry.getValue() == 0) {
            // s += entry.getKey();
            // s += ", ";
            // }
            // }
            // log(s);
            covLen = newCov;
        }
    }

    @Override
    public void exit() {
        updateCov("exit");
        // try {
        // biasClient.close();
        // client.close();
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
    }

    @Override
    public double getZ() {
        updateCov("getZ");
        NanonisClient client = clients.getClient();
        try {
            return client.ZCtrlZPosGet();
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
        return 0;
    }

    private boolean tipIsMoving = false;

    @Override
    // Coords are window-space: (-1 <-> 1) within the current scanning window
    public void moveTipTo(double x, double y) {
        log("MoveTipTo " + x + "," + y);
        tipIsMoving = true;
        double[] xy = ABDController.imageToScannerCoords(this, new double[] { x, y });
        new Thread() {
            public void run() {
                NanonisClient client = clients.getClient();
                try {
                    client.FolMeXYPosSet(xy[0] / 1e9, xy[1] / 1e9, true);
                    tipIsMoving = false;
                    log("MoveTipTo move done");
                } catch (IOException | NanonisException | ResponseException e) {
                    e.printStackTrace();
                } finally {
                    clients.returnClient(client);
                }
            }
        }.start();
    }

    @Override
    public boolean tipIsMoving() {
        updateCov("tipIsMoving");
        log("tipIsMoving: " + tipIsMoving);
        return tipIsMoving;
    }

    @Override
    // Returned coords are window-space: (-1 <-> 1) within the current scanning
    // window
    public double[] getTipPosition() {
        double[] xy = new double[] { 0, 0 };
        NanonisClient client = clients.getClient();
        try {
            xy = client.FolMeXYPosGet(true);
            xy[0] *= 1e9;
            xy[1] *= 1e9;
            xy = ABDController.scannerToImageCoords(this, xy);
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
        log("Tip Pos: " + xy[0] + "," + xy[1]);
        return xy;
    }

    @Override
    public void setTipSpeed(double s) {
        updateCov("setTipSpeed");
        NanonisClient client = clients.getClient();
        try {
            client.ScanSpeedSet(
                    (float) (s / 1e9),
                    (float) (s / 1e9),
                    0f,
                    0f,
                    1,
                    1f);
        } catch (IOException | NanonisException | ResponseException | UnsignedException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    @Override
    public double getTipSpeed() {
        updateCov("getTipSpeed");
        double speed = 0;
        NanonisClient client = clients.getClient();
        try {
            ScanSpeed speedSetting = client.ScanSpeedGet();
            speed = (double) Math.round(speedSetting.fSpeedMps() * 1e12f) / 1e3d;
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
        return speed;
    }

    @Override
    // Returned coords are physical-space
    public double[] getTipScanPosition() {
        double[] xy = new double[] { 0, 0 };
        NanonisClient client = clients.getClient();
        try {
            xy = client.FolMeXYPosGet(true);
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
        log("Scan Pos: " + xy[0] + "," + xy[1]);
        return xy;
    }

    @Override
    public void setZOffset(double offset) {
        updateCov("setZOffset");
        // I think this function wants to move the tip to a height relative to the
        // surface level when the feedback was turned off
        if (!surfaceHeightWas.isPresent()) {
            System.err.println("[NANONIS] Err: setZOffset can only run if the feedback controller is off");
        }
        double newHeight = surfaceHeightWas.get().doubleValue() + offset;
        NanonisClient client = clients.getClient();
        try {
            client.ZCtrlZPosSet((float) newHeight);
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    @Override
    public double getBias() {
        updateCov("getBias");
        double bias = 0;
        NanonisClient client = clients.getClient();
        try {
            bias = client.BiasGet();
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
        return bias;
    }

    @Override
    public void setBias(double Vb) {
        updateCov("setBias");
        NanonisClient client = clients.getClient();
        try {
            client.BiasSet((float) Vb);
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    @Override
    public BiasSignal getBiasSignal() {
        updateCov("getBiasSignal");
        return this.biasSignal;
    }

    private boolean lithoModulation = false;

    @Override
    public void setLithoModulation(boolean b) {
        updateCov("setLithoModulation");
        lithoModulation = b;
    }

    @Override
    public boolean getLithoModulation() {
        updateCov("getLithoModulation");
        return lithoModulation;
    }

    @Override
    public double getCurrent() {
        updateCov("getCurrent");
        float setpoint = 0;
        NanonisClient client = clients.getClient();
        try {
            setpoint = client.ZCtrlSetpntGet();
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
        return setpoint;
    }

    @Override
    public double getMeasuredCurrent() {
        updateCov("getMeasuredCurrent");
        NanonisClient client = clients.getClient();
        try {
            return client.CurrentGet();
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
        return 0;
    }

    @Override
    public void setCurrent(double I) {
        updateCov("setCurrent");
        NanonisClient client = clients.getClient();
        try {
            client.ZCtrlSetpntSet((float) I);
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    @Override
    public CurrentSignal getCurrentSignal() {
        updateCov("getCurrentSignal");
        return this.currentSignal;
    }

    private Optional<Double> surfaceHeightWas = Optional.empty();

    @Override
    public void setFeedback(boolean fb) {
        updateCov("setFeedback");
        NanonisClient client = clients.getClient();
        try {
            if (fb) {
                surfaceHeightWas = Optional.empty();
            } else {
                surfaceHeightWas = Optional.of(getZ());
            }
            client.ZCtrlOnOffSet(fb);
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    private boolean allowPreampRangeChange = false;

    @Override
    public void setAllowPreampRangeChange(boolean b) {
        updateCov("setAllowPreampRangeChange");
        allowPreampRangeChange = b;
    }

    @Override
    public boolean getAllowPreampRangeChange() {
        updateCov("getAllowPreampRangeChange");
        return allowPreampRangeChange;
    }

    @Override
    public double getScanWidth() {
        updateCov("getScanWidth");
        double width = 0;
        NanonisClient client = clients.getClient();
        try {
            scanFrame.maybeLoad();
            width = (double) Math.round(scanFrame.widthM * 1e12f) / 1e3d;
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
        return width;
    }

    @Override
    public void setScanWidth(double w) {
        updateCov("setScanWidth");
        NanonisClient client = clients.getClient();
        try {
            scanFrame.widthM = ((float) (w / 1e9));
            scanFrame.maybeStore();
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    @Override
    public double getScanHeight() {
        updateCov("getScanHeight");
        double height = 0;
        NanonisClient client = clients.getClient();
        try {
            scanFrame.maybeLoad();
            height = (double) Math.round(scanFrame.heightM * 1e12f) / 1e3d;
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
        return height;
    }

    @Override
    public void setScanHeight(double h) {
        updateCov("setScanHeight");
        NanonisClient client = clients.getClient();
        try {
            scanFrame.heightM = ((float) (h / 1e9));
            scanFrame.maybeStore();
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    @Override
    public int getPointsPerLine() {
        updateCov("getPointsPerLine");
        int ppl = 0;
        NanonisClient client = clients.getClient();
        try {
            ScanBuffer scanBuffer = client.ScanBufferGet();
            ppl = scanBuffer.pixelsPerLine();
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
        return ppl;
    }

    @Override
    public void setPointsPerLine(int p) {
        updateCov("setPointsPerLine");
        NanonisClient client = clients.getClient();
        try {
            ScanBuffer scanBuffer = client.ScanBufferGet();
            client.ScanBufferSet(
                    scanBuffer.channelIndexes(),
                    p,
                    scanBuffer.linesPerScan());
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    @Override
    public int getNumLines() {
        updateCov("getNumLines");
        int numLines = 0;
        NanonisClient client = clients.getClient();
        try {
            ScanBuffer scanBuffer = client.ScanBufferGet();
            numLines = scanBuffer.linesPerScan();
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
        return numLines;
    }

    @Override
    public void setNumLines(int n) {
        updateCov("setNumLines");
        NanonisClient client = clients.getClient();
        try {
            ScanBuffer scanBuffer = client.ScanBufferGet();
            client.ScanBufferSet(
                    scanBuffer.channelIndexes(),
                    scanBuffer.pixelsPerLine(),
                    n);
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    @Override
    public int getScanAngle() {
        updateCov("getScanAngle");
        int angle = 0;
        NanonisClient client = clients.getClient();
        try {
            scanFrame.maybeLoad();
            angle = (int) -scanFrame.angleDeg;
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
        return angle;
    }

    @Override
    public void setScanAngle(int angle) {
        updateCov("setScanAngle");
        NanonisClient client = clients.getClient();
        try {
            scanFrame.angleDeg = -angle;
            scanFrame.maybeStore();
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    @Override
    public double[] getScanCenter() {
        updateCov("getScanCenter");
        double[] center = new double[] { 0, 0 };
        NanonisClient client = clients.getClient();
        try {
            scanFrame.maybeLoad();
            float x = scanFrame.centerXM;
            float y = scanFrame.centerYM;
            center[0] = (double) Math.round(x * 1e12f) / 1e3d;
            center[1] = (double) Math.round(y * 1e12f) / 1e3d;
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
        return center;
    }

    @Override
    public void setScanCenter(double x, double y) {
        updateCov("setScanCenter");
        NanonisClient client = clients.getClient();
        try {
            scanFrame.centerXM = ((float) (x / 1e9));
            scanFrame.centerYM = ((float) (y / 1e9));
            scanFrame.maybeStore();
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    @Override
    public double getScanRangeWidth() {
        updateCov("getScanRangeWidth");
        double width = 0;
        NanonisClient client = clients.getClient();
        try {
            width = client.PiezoRangeGet()[0] * 1e9;
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
        return width;
    }

    @Override
    public double getScanRangeHeight() {
        updateCov("getScanRangeHeight");
        double height = 0;
        NanonisClient client = clients.getClient();
        try {
            height = client.PiezoRangeGet()[1] * 1e9;
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
        return height;
    }

    private boolean isScanning = false;
    private boolean scanDirUp = false;

    @Override
    public void startUpScan() {
        updateCov("startUpScan");
        NanonisClient client = clients.getClient();
        try {
            isScanning = true;
            client.ScanAction(ScanAction.START, true);
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
        if (reportScanLines)
            new Thread() {
                public void run() {
                    int lastLine = -1;
                    while (isScanning) {
                        ScanData scanData;
                        NanonisClient client = clients.getClient();
                        try {
                            scanData = client.ScanFrameDataGrab(14, true);
                        } catch (Exception e) {
                            e.printStackTrace();
                            continue;
                        } finally {
                            clients.returnClient(client);
                        }
                        scanDirUp = scanData.scanDirUp();
                        int line = computeNewestLine(scanData);
                        if (line != lastLine && line != -1) {
                            drawNewLines(lastLine, line, scanData.data());
                            lastLine = line;
                        }
                    }
                }
            }.start();

    }

    private void drawNewLines(int lastLine, int line, float[][] scanData) {
        int dir;
        if (lastLine == -1) {
            dir = 1;
            lastLine = line - 1;
        } else {
            dir = (line > lastLine) ? 1 : -1;
        }
        int cols = scanData[0].length;
        int rows = scanData.length;
        for (int i = lastLine; i != line; i += dir) {
            int lineNum = i + dir;
            double[] data = new double[cols];
            for (int j = 0; j < cols; j++) {
                data[j] = scanData[lineNum][j];
            }
            ABDReverseClient.drawLine(rows - lineNum - 1, data);
        }
    }

    private int computeNewestLine(ScanData scanData) {
        int rows = scanData.data().length;
        int cols = scanData.data()[0].length;
        if (scanData.scanDirUp()) {
            for (int i = 0; i < rows; i++) {
                if (!Float.isNaN(scanData.data()[i][cols - 1])) {
                    return i;
                }
            }
        } else {
            for (int i = rows - 1; i >= 0; i--) {
                if (!Float.isNaN(scanData.data()[i][cols - 1])) {
                    return i;
                }
            }

        }
        return -1;
    }

    @Override
    public void stopScan() {
        updateCov("stopScan");
        NanonisClient client = clients.getClient();
        try {
            isScanning = false;
            client.ScanAction(ScanAction.STOP, false);
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    @Override
    public boolean upScanning() {
        updateCov("upScanning");
        return scanDirUp;
    }

    @Override
    public boolean isScanning() {
        updateCov("isScanning");
        return isScanning;
    }

    @Override
    public void withdraw() {
        updateCov("withdraw");
        NanonisClient client = clients.getClient();
        try {
            client.ZCtrlWithdraw(true, -1);
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    @Override
    public void autoApproach() {
        updateCov("autoApproach");
        NanonisClient client = clients.getClient();
        try {
            client.AutoApproachOpen();
            client.AutoApproachOnOffSet(true);
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    @Override
    public void engage() {
        updateCov("engage");
        setFeedback(true);
    }

    @Override
    public void setCoarseAmplitude(int amp) {
        updateCov("setCoarseAmplitude");
        NanonisClient client = clients.getClient();
        try {
            for (int axis = 1; axis <= 3; axis++) {
                float[] freqAmp = client.MotorFreqAmpGet(axis);
                client.MotorFreqAmpSet(freqAmp[0], amp, axis);
            }
        } catch (IOException | NanonisException | ResponseException | UnsignedException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    private int courseSteps = 0;

    @Override
    public void setCoarseSteps(int steps) {
        updateCov("setCoarseSteps");
        courseSteps = steps;
    }

    @Override
    public int getCoarseStepIncrement() {
        updateCov("getCoarseStepIncrement");
        // TODO: this is what the matrix implementation does
        // I have no idea what it's for
        return 10;
    }

    @Override
    public void retract() {
        updateCov("retract");
        NanonisClient client = clients.getClient();
        try {
            client.MotorStartMove(Direction.Z_POS, courseSteps, courseGroup, true);
        } catch (IOException | NanonisException | ResponseException | UnsignedException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    @Override
    public void coarseApproach() {
        updateCov("coarseApproach");
        NanonisClient client = clients.getClient();
        try {
            client.MotorStartMove(Direction.Z_NEG, courseSteps, courseGroup, true);
        } catch (IOException | NanonisException | ResponseException | UnsignedException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    @Override
    public void moveXPlus() {
        updateCov("moveXPlus");
        NanonisClient client = clients.getClient();
        try {
            client.MotorStartMove(Direction.X_POS, courseSteps, courseGroup, true);
        } catch (IOException | NanonisException | ResponseException | UnsignedException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    @Override
    public void moveXMinus() {
        updateCov("moveXMinus");
        NanonisClient client = clients.getClient();
        try {
            client.MotorStartMove(Direction.X_NEG, courseSteps, courseGroup, true);
        } catch (IOException | NanonisException | ResponseException | UnsignedException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    @Override
    public void moveYPlus() {
        updateCov("moveYPlus");
        NanonisClient client = clients.getClient();
        try {
            client.MotorStartMove(Direction.Y_POS, courseSteps, courseGroup, true);
        } catch (IOException | NanonisException | ResponseException | UnsignedException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    @Override
    public void moveYMinus() {
        updateCov("moveYMinus");
        NanonisClient client = clients.getClient();
        try {
            client.MotorStartMove(Direction.Y_NEG, courseSteps, courseGroup, true);
        } catch (IOException | NanonisException | ResponseException | UnsignedException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    @Override
    public void setContinuousScanEnabled(boolean b) {
        updateCov("setContinuousScanEnabled");
        NanonisClient client = clients.getClient();
        try {
            ScanProps props = client.ScanPropsGet();
            client.ScanPropsSet(
                    b,
                    props.bouncyScan(),
                    props.autoSave(),
                    props.seriesName(),
                    props.comment(),
                    new String[0]);
        } catch (IOException | NanonisException | ResponseException | UnsignedException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    @Override
    public boolean isContinuousScanEnabled() {
        updateCov("isContinuousScanEnabled");
        NanonisClient client = clients.getClient();
        try {
            return client.ScanPropsGet().continuousScan();
        } catch (IOException | NanonisException | ResponseException | UnsignedException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
        return false;
    }

    private boolean reportScanLines = false;

    @Override
    public void setReportScanLines(boolean b) {
        updateCov("setReportScanLines");
        reportScanLines = b;
    }

    @Override
    public boolean isReportingScanLines() {
        updateCov("isReportingScanLines");
        return reportScanLines;
    }

    @Override
    public void zRamp() {
        updateCov("zRamp");
        NanonisClient client = clients.getClient();
        try {
            client.TipShaperStart(true, -1);
        } catch (IOException | NanonisException | ResponseException | UnsignedException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    @Override
    public void vPulse(float voltage, float width) {
        updateCov("vPulse");
        NanonisClient client = clients.getClient();
        try {
            client.BiasPulse(true, width, voltage, 0, 0);
        } catch (IOException | NanonisException | ResponseException | UnsignedException e) {
            e.printStackTrace();
        } finally {
            clients.returnClient(client);
        }
    }

    @Override
    public void setLithoConditions(boolean b) {
        updateCov("setLithoConditions");
        // TODO: matrix does STM_AtomManipulation::Sampler_I.Enable and Disable here
    }
}