package controllers;

import java.io.IOException;
import java.util.Optional;

import com.ABDReverseClient;

import controllers.nanonis.NanonisClient;
import controllers.nanonis.NanonisException;
import controllers.nanonis.ResponseException;
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

    private NanonisClient client;
    private NanonisClient biasClient;
    private NanonisClient tipMoveClient;

    private BiasSignal biasSignal;
    private CurrentSignal currentSignal;

    private static boolean log = true;

    private int courseGroup = 0;

    public NanonisController() {
        super();

        try {
            client = new NanonisClient("127.0.0.1", 6501);
            biasClient = new NanonisClient("127.0.0.1", 6502);
            tipMoveClient = new NanonisClient("127.0.0.1", 6503);
        } catch (IOException e) {
            e.printStackTrace();
        }

        biasSignal = new BiasSignal(this);
        biasSignal.units = "V";
        biasSignal.stepSize = 0.2; // V
        biasSignal.stepTime = 1; // ms

        currentSignal = new CurrentSignal(this);
        currentSignal.units = "nA";
        currentSignal.stepSize = 1; // nA
        currentSignal.stepTime = 1; // ms
    }

    private void log(String message) {
        if (log) {
            System.out.println("[NANONIS] " + message);
        }
    }

    @Override
    public void exit() {
        try {
            biasClient.close();
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public double getZ() {
        try {
            return client.ZCtrlZPosGet();
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
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
                try {
                    tipMoveClient.FolMeXYPosSet(xy[0] / 1e9, xy[1] / 1e9, true);
                    tipIsMoving = false;
                    log("MoveTipTo move done");
                } catch (IOException | NanonisException | ResponseException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    public boolean tipIsMoving() {
        log("tipIsMoving: " + tipIsMoving);
        return tipIsMoving;
    }

    @Override
    // Returned coords are window-space: (-1 <-> 1) within the current scanning
    // window
    public double[] getTipPosition() {
        double[] xy = new double[] { 0, 0 };
        try {
            xy = client.FolMeXYPosGet(false);
            xy[0] *= 1e9;
            xy[1] *= 1e9;
            xy = ABDController.scannerToImageCoords(this, xy);
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
        log("Tip Pos: " + xy[0] + "," + xy[1]);
        return xy;
    }

    @Override
    public void setTipSpeed(double s) {
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
        }
    }

    @Override
    public double getTipSpeed() {
        double speed = 0;
        try {
            ScanSpeed speedSetting = client.ScanSpeedGet();
            speed = (double) Math.round(speedSetting.fSpeedMps() * 1e12f) / 1e3d;
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
        return speed;
    }

    @Override
    // Returned coords are physical-space
    public double[] getTipScanPosition() {
        double[] xy = new double[] { 0, 0 };
        try {
            xy = client.FolMeXYPosGet(false);
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
        log("Scan Pos: " + xy[0] + "," + xy[1]);
        return xy;
    }

    @Override
    public void setZOffset(double offset) {
        // I think this function wants to move the tip to a height relative to the
        // surface level when the feedback was turned off
        if (!surfaceHeightWas.isPresent()) {
            System.err.println("[NANONIS] Err: setZOffset can only run if the feedback controller is off");
        }
        double newHeight = surfaceHeightWas.get().doubleValue() + offset;
        try {
            client.ZCtrlZPosSet((float) newHeight);
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public double getBias() {
        double bias = 0;
        try {
            bias = biasClient.BiasGet();
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
        return bias;
    }

    @Override
    public void setBias(double Vb) {
        try {
            client.BiasSet((float) Vb);
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public BiasSignal getBiasSignal() {
        return this.biasSignal;
    }

    private boolean lithoModulation = false;

    @Override
    public void setLithoModulation(boolean b) {
        lithoModulation = b;
    }

    @Override
    public boolean getLithoModulation() {
        return lithoModulation;
    }

    @Override
    public double getCurrent() {
        float setpoint = 0;
        try {
            setpoint = client.ZCtrlSetpntGet();
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
        return setpoint;
    }

    @Override
    public double getMeasuredCurrent() {
        try {
            return client.CurrentGet();
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void setCurrent(double I) {
        try {
            client.ZCtrlSetpntSet((float) I);
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CurrentSignal getCurrentSignal() {
        return this.currentSignal;
    }

    private Optional<Double> surfaceHeightWas = Optional.empty();

    @Override
    public void setFeedback(boolean fb) {
        try {
            if (fb) {
                surfaceHeightWas = Optional.empty();
            } else {
                surfaceHeightWas = Optional.of(getZ());
            }
            client.ZCtrlOnOffSet(fb);
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
    }

    private boolean allowPreampRangeChange = false;
    @Override
    public void setAllowPreampRangeChange(boolean b) {
        allowPreampRangeChange = b;
    }

    @Override
    public boolean getAllowPreampRangeChange() {
        return allowPreampRangeChange;
    }

    @Override
    public double getScanWidth() {
        double width = 0;
        try {
            client.scanFrame.maybeLoad();
            width = (double) Math.round(client.scanFrame.widthM * 1e12f) / 1e3d;
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
        return width;
    }

    @Override
    public void setScanWidth(double w) {
        try {
            client.scanFrame.widthM = ((float) (w / 1e9));
            client.scanFrame.maybeStore();
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public double getScanHeight() {
        double height = 0;
        try {
            client.scanFrame.maybeLoad();
            height = (double) Math.round(client.scanFrame.heightM * 1e12f) / 1e3d;
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
        return height;
    }

    @Override
    public void setScanHeight(double h) {
        try {
            client.scanFrame.heightM = ((float) (h / 1e9));
            client.scanFrame.maybeStore();
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getPointsPerLine() {
        int ppl = 0;
        try {
            ScanBuffer scanBuffer = client.ScanBufferGet();
            ppl = scanBuffer.pixelsPerLine();
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
        return ppl;
    }

    @Override
    public void setPointsPerLine(int p) {
        try {
            ScanBuffer scanBuffer = client.ScanBufferGet();
            client.ScanBufferSet(
                    scanBuffer.channelIndexes(),
                    p,
                    scanBuffer.linesPerScan());
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getNumLines() {
        int numLines = 0;
        try {
            ScanBuffer scanBuffer = client.ScanBufferGet();
            numLines = scanBuffer.linesPerScan();
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
        return numLines;
    }

    @Override
    public void setNumLines(int n) {
        try {
            ScanBuffer scanBuffer = client.ScanBufferGet();
            client.ScanBufferSet(
                    scanBuffer.channelIndexes(),
                    scanBuffer.pixelsPerLine(),
                    n);
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getScanAngle() {
        int angle = 0;
        try {
            client.scanFrame.maybeLoad();
            angle = (int) -client.scanFrame.angleDeg;
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
        return angle;
    }

    @Override
    public void setScanAngle(int angle) {
        try {
            client.scanFrame.angleDeg = -angle;
            client.scanFrame.maybeStore();
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public double[] getScanCenter() {
        double[] center = new double[] { 0, 0 };
        try {
            client.scanFrame.maybeLoad();
            float x = client.scanFrame.centerXM;
            float y = client.scanFrame.centerYM;
            center[0] = (double) Math.round(x * 1e12f) / 1e3d;
            center[1] = (double) Math.round(y * 1e12f) / 1e3d;
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
        return center;
    }

    @Override
    public void setScanCenter(double x, double y) {
        try {
            client.scanFrame.centerXM = ((float) (x / 1e9));
            client.scanFrame.centerYM = ((float) (y / 1e9));
            client.scanFrame.maybeStore();
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public double getScanRangeWidth() {
        double width = 0;
        try {
            width = client.PiezoRangeGet()[0] * 1e9;
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
        return width;
    }

    @Override
    public double getScanRangeHeight() {
        double height = 0;
        try {
            height = client.PiezoRangeGet()[1] * 1e9;
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
        return height;
    }

    private boolean isScanning = false;
    private boolean scanDirUp = false;

    @Override
    public void startUpScan() {
        try {
            isScanning = true;
            client.ScanAction(ScanAction.START, true);
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
        new Thread() {
            public void run() {
                int lastLine = -1;
                try {
                    while (isScanning) {
                        ScanData scanData = client.ScanFrameDataGrab(14, true);
                        int currentLine = computeNewestLine(scanData);
                        scanDirUp = scanData.scanDirUp();
                        if (currentLine != lastLine && currentLine >= 0) {
                            int rows = scanData.data().length;
                            drawScanLine(rows - currentLine - 1, scanData.data()[currentLine]);
                            lastLine = currentLine;
                        }
                    }
                } catch (IOException | NanonisException | ResponseException | UnsignedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
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

    private void drawScanLine(int idx, float[] data) {
        StringBuilder s = new StringBuilder();

        s.append(data[0]);
        for (int i = 0; i < data.length; i++) {
            s.append(",");
            s.append(data[i]);
        }
        ABDReverseClient.command("L:" + idx + ":" + s.toString());
    }

    @Override
    public void stopScan() {
        try {
            isScanning = false;
            client.ScanAction(ScanAction.STOP, false);
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean upScanning() {
        return scanDirUp;
    }

    @Override
    public boolean isScanning() {
        return isScanning;
    }

    @Override
    public void withdraw() {
        try {
            client.ZCtrlWithdraw(true, -1);
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void autoApproach() {
        try {
            client.AutoApproachOpen();
            client.AutoApproachOnOffSet(true);
        } catch (IOException | NanonisException | ResponseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void engage() {
        setFeedback(true);
    }

    @Override
    public void setCoarseAmplitude(int amp) {
        try {
            for (int axis = 1; axis <= 3; axis++) {
                float[] freqAmp = client.MotorFreqAmpGet(axis);
                client.MotorFreqAmpSet(freqAmp[0], amp, axis);
            }
        } catch (IOException | NanonisException | ResponseException | UnsignedException e) {
            e.printStackTrace();
        }
    }

    private int courseSteps = 0;

    @Override
    public void setCoarseSteps(int steps) {
        courseSteps = steps;
    }

    @Override
    public int getCoarseStepIncrement() {
        // TODO: this is what the matrix implementation does
        // I have no idea what it's for
        return 10;
    }

    @Override
    public void retract() {
        try {
            client.MotorStartMove(Direction.Z_POS, courseSteps, courseGroup, true);
        } catch (IOException | NanonisException | ResponseException | UnsignedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void coarseApproach() {
        try {
            client.MotorStartMove(Direction.Z_NEG, courseSteps, courseGroup, true);
        } catch (IOException | NanonisException | ResponseException | UnsignedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void moveXPlus() {
        try {
            client.MotorStartMove(Direction.X_POS, courseSteps, courseGroup, true);
        } catch (IOException | NanonisException | ResponseException | UnsignedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void moveXMinus() {
        try {
            client.MotorStartMove(Direction.X_NEG, courseSteps, courseGroup, true);
        } catch (IOException | NanonisException | ResponseException | UnsignedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void moveYPlus() {
        try {
            client.MotorStartMove(Direction.Y_POS, courseSteps, courseGroup, true);
        } catch (IOException | NanonisException | ResponseException | UnsignedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void moveYMinus() {
        try {
            client.MotorStartMove(Direction.Y_NEG, courseSteps, courseGroup, true);
        } catch (IOException | NanonisException | ResponseException | UnsignedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setContinuousScanEnabled(boolean b) {
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
        }
    }

    @Override
    public boolean isContinuousScanEnabled() {
        try {
            return client.ScanPropsGet().continuousScan();
        } catch (IOException | NanonisException | ResponseException | UnsignedException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean reportScanLines = false;

    @Override
    public void setReportScanLines(boolean b) {
        reportScanLines = b;
    }

    @Override
    public boolean isReportingScanLines() {
        return reportScanLines;
    }

    @Override
    public void zRamp() {
        try {
            client.TipShaperStart(true, -1);
        } catch (IOException | NanonisException | ResponseException | UnsignedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void vPulse() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'vPulse'");
    }

    @Override
    public void setLithoConditions(boolean b) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setLithoConditions'");
    }
}