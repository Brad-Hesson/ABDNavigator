package navigator;

//import BufferedSTMImage;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.w3c.dom.Element;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import main.ABDPythonAPIClient;
import main.SampleNavigator;
import util.FFT2D;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

public class NanonisSTMImageLayer extends ImageLayer {
	private float[][] forwardZData;
	private float[][] backwardZData;
	private float[][] forwardCurrentData;
	private float[][] backwardCurrentData;

	private int pixWidth = 0;
	private int pixHeight = 0;

	private static BufferedSTMImage bImg = null;

	private String imageChannel = "Z Forward";

	private float minZFraction = 0;
	private float maxZFraction = 1;

	private int colorSchemeIdx = 0;

	public int maximaThreshold = 500;
	public int maximaPrecision = 1;
	public double maximaExpectedDiameter = 1;
	public GroupLayer replaceGroup = null;

	public double expectedLatticeSpacing = 0.77;
	public double spacingUncertainty = 0.2;
	public GroupLayer replaceLattice = null;

	public double latticeAngle = 0;
	public double detectionContrast = 0.6;
	public double predictionThreshold = 0.5;

	public boolean paramsExtracted = false;
	public double zFactor = 1;
	private double bias = 2; // V
	private double current = 0.01; // nA
	public double scaleX0 = 100; // nm
	public double scaleY0 = 100; // nm
	public double angle0 = 0; // deg

	public NanonisSTMImageLayer() {
		super();
		// appendActions( new
		// String[]{"imageLeftRight","imageUpDown","togglePlaneSubtract","toggleLineByLineFlatten","nextColorScheme","locateMaxima","locateLattice","addExample"}
		// );
		// appendActions( new
		// String[]{"locateMaxima","locateLattice","altLocateLattice","addExample","clearExamples","checkTipQuality"}
		// );
		appendActions(new String[] { "altLocateLattice", "addExample", "clearExamples", "checkTipQuality" });
		// tabs.put("maxima", new String[]
		// {"locateMaxima","maximaExpectedDiameter","maximaPrecision","maximaThreshold"});
		// tabs.put("lattice", new String[]
		// {"locateLattice","altLocateLattice","latticeExpectedSpacing","latticeSpacingUncertainty"});
		tabs.put("detection",
				new String[] { "checkTipQuality", "latticeAngle", "detectionContrast", "predictionThreshold" });
		tabs.put("lattice", new String[] { "altLocateLattice", "latticeExpectedSpacing", "latticeSpacingUncertainty" });
		// tabs.put("machine learning", new String[] {"addExample","clearExamples"});
		tabs.put("training", new String[] { "addExample", "clearExamples" });
		tabs.put("settings", new String[] { "sampleBias", "current" });
		categories.put("colorSchemeIndex", new String[] { "0", "1", "2", "3" });
		categories.put("imageChannel", new String[] { "Z Forward", "Z Backward", "Current Forward", "Current Backward" });
		categories.put("lineByLineFlatten", new String[] { "true", "false" });
		categories.put("planeSubtract", new String[] { "true", "false" });
		units.put("latticeExpectedSpacing", "nm");
		units.put("latticeSpacingUncertainty", "nm");
		units.put("latticeAngle", "deg");
		// units.put("maximaExpectedDiameter", "nm");
		units.put("sampleBias", "V");
		units.put("current", "nA");
	}

	public void handleVisibilityChange() {
		if ((getAncestorVisibility()) && (!imageLoaded)) {

			// System.out.println("initializing image...");
			init();
		}

		super.handleVisibilityChange();
	}

	public boolean imageLoaded = false;

	public void init() {
		init(false);
	}

	public void init(boolean forceLoad) {
		listenToParentVisibility();

		if ((!getAncestorVisibility()) && (!forceLoad)) {
			// System.out.println(imgName + " is not visible");
			return;
		}

		if (forceLoad) {
			System.out.println("force loading image");
		}

		String imgNameString = new String(imgName);
		imgNameString = imgNameString.replaceFirst("file:", "file:" + SampleNavigator.relativeDirectory);
		System.out.println("image name: " + imgNameString);
		String fullFileName = imgNameString.replaceFirst("file:", "");
		SampleNavigator.linkRegistry.add(fullFileName);

		NanonisSXMFile sxm;
		try {
			sxm = new NanonisSXMFile(fullFileName);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		zFactor = 1;
		forwardZData = imageTranspose(sxm.images.get("Z")[0]);
		backwardZData = imageFlip(imageTranspose(sxm.images.get("Z")[1]));
		forwardCurrentData = imageTranspose(sxm.images.get("Current")[0]);
		backwardCurrentData = imageFlip(imageTranspose(sxm.images.get("Current")[1]));
		currentImageData = forwardZData;
		scaleX0 = Float.parseFloat(sxm.params.get("SCAN_RANGE").trim().split("\s+")[0]) * 1e9;
		scaleY0 = Float.parseFloat(sxm.params.get("SCAN_RANGE").trim().split("\s+")[1]) * 1e9;
		angle0 = Float.parseFloat(sxm.params.get("SCAN_ANGLE").trim());
		bias = Double.parseDouble(sxm.params.get("BIAS").trim());
		current = Double.parseDouble(
				sxm.params.get("Z-CONTROLLER")
						.trim()
						.lines()
						.skip(1)
						.findFirst()
						.get()
						.strip()
						.split("[\t\s]+")[3])
				* 1e9;
		paramsExtracted = true;
		nmFromIdx = scaleX0 / pixWidth * 1e9;
		nmFromZ = 1e9 / zFactor;
		pixWidth = sxm.xPixels;
		pixHeight = sxm.yPixels;
		imageLoaded = true;

		// need to init with a dummy image first
		float[][] fData = new float[pixWidth][pixHeight];
		bImg = new BufferedSTMImage(fData);
		initFromImage(SwingFXUtils.toFXImage(bImg, null));

		// the actual image gets loaded here
		setImageChannel(imageChannel);

		planeFitOnInit();
	}

	private float[][] imageTranspose(float[][] image) {
		int X = image.length;
		int Y = image[0].length;
		float[][] out = new float[Y][X];
		for (int i = 0; i < X; i++) {
			for (int j = 0; j < Y; j++) {
				out[j][i] = image[i][j];
			}
		}
		return out;
	}

	private float[][] imageFlip(float[][] image) {
		int rows = image.length;
		float[] tempRow;
		for (int i = 0; i < rows / 2; i++) {
			tempRow = image[i];
			image[i] = image[rows - i - 1];
			image[rows - i - 1] = tempRow;
		}
		return image;
	}

	public void setImageChannel(String s) {
		imageChannel = s;
		if (s.equalsIgnoreCase("Z Forward")) {
			setImageTo(forwardZData);
		} else if (s.equalsIgnoreCase("Z Backward")) {
			setImageTo(backwardZData);
		} else if (s.equalsIgnoreCase("Current Forward")) {
			setImageTo(forwardCurrentData);
		} else if (s.equalsIgnoreCase("Current Backward")) {
			setImageTo(backwardCurrentData);
		}
	}

	public boolean planeSubtract = true;

	public void togglePlaneSubtract() {
		planeSubtract = !planeSubtract;
		setImageTo(currentImageData);
		SampleNavigator.refreshAttributeEditor();
	}

	public boolean lineByLineFlatten = false;

	public void toggleLineByLineFlatten() {
		lineByLineFlatten = !lineByLineFlatten;
		setImageTo(currentImageData);
		SampleNavigator.refreshAttributeEditor();
	}

	public float[][] currentImageData = null;

	public void setImageTo(float[][] data) {
		if (data == null)
			return;

		float[][] fData = imageClone(data);

		if (planeSubtract) {
			// if also doing a line by line flatten, do that both before and after the plane
			// subtract
			if (lineByLineFlatten)
				imageLineSubtract(fData);

			imagePlaneSubtract(fData);
		}

		if (lineByLineFlatten)
			imageLineSubtract(fData);

		imageNormalize(fData);

		bImg = new BufferedSTMImage(fData);
		bImg.colorSchemeIdx = this.colorSchemeIdx;
		bImg.minZFraction = this.minZFraction;
		bImg.maxZFraction = this.maxZFraction;
		bImg.processData = false;
		bImg.draw();

		setImage(SwingFXUtils.toFXImage(bImg, null));
		currentImageData = data;
	}

	private float[][] imageClone(float[][] image) {
		int width = image.length;
		int height = image[0].length;
		float[][] fData = new float[width][height];
		for (int yIdx = 0; yIdx < height; yIdx++) {
			for (int xIdx = 0; xIdx < width; xIdx++) {
				fData[xIdx][yIdx] = image[xIdx][yIdx];
			}
		}
		return fData;
	}

	private void imagePlaneSubtract(float[][] image) {
		int width = image.length;
		int height = image[0].length;
		double dzdxAve = 0;
		double dzdyAve = 0;

		// now do the plane subtract
		for (int yIdx = 0; yIdx < height; yIdx++) {
			dzdxAve += image[width - 1][yIdx] - image[0][yIdx];
		}
		for (int xIdx = 0; xIdx < width; xIdx++) {
			dzdyAve += image[xIdx][height - 1] - image[xIdx][0];
		}
		dzdxAve /= width * height;
		dzdyAve /= width * height;
		for (int yIdx = 0; yIdx < height; yIdx++) {
			for (int xIdx = 0; xIdx < width; xIdx++) {
				image[xIdx][yIdx] -= (float) (dzdxAve * xIdx + dzdyAve * yIdx);
			}
		}
	}

	private void imageLineSubtract(float[][] image) {
		int width = image.length;
		int height = image[0].length;
		float[] diffs = new float[width];
		for (int yIdx = 0; yIdx < height - 1; yIdx++) {
			for (int xIdx = 0; xIdx < width; xIdx++) {
				diffs[xIdx] = image[xIdx][yIdx + 1] - image[xIdx][yIdx];
			}

			float median = median(diffs);

			for (int xIdx = 0; xIdx < width; xIdx++) {
				image[xIdx][yIdx + 1] -= median;
			}
		}
	}

	private void imageNormalize(float[][] image) {
		int width = image.length;
		int height = image[0].length;
		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;
		for (int xIdx = 0; xIdx < width; xIdx++) {
			for (int yIdx = 0; yIdx < height; yIdx++) {
				min = Math.min(min, image[xIdx][yIdx]);
				max = Math.max(max, image[xIdx][yIdx]);
			}
		}
		for (int xIdx = 0; xIdx < width; xIdx++) {
			for (int yIdx = 0; yIdx < height; yIdx++) {
				image[xIdx][yIdx] = (image[xIdx][yIdx] - min) / (max - min);
			}
		}
	}

	public void setFromXML(Element xml, boolean deep) {

		super.setFromXML(xml, deep);

		String s = xml.getAttribute("maxZFraction");
		if (s.length() > 0)
			maxZFraction = Float.parseFloat(s);
		s = xml.getAttribute("minZFraction");
		if (s.length() > 0)
			minZFraction = Float.parseFloat(s);

		s = xml.getAttribute("planeSubtract");
		if (s.length() > 0)
			planeSubtract = Boolean.parseBoolean(s);
		s = xml.getAttribute("lineByLineFlatten");
		if (s.length() > 0)
			lineByLineFlatten = Boolean.parseBoolean(s);

		s = xml.getAttribute("imageChannel");
		if (s.length() > 0)
			imageChannel = new String(s);

		s = xml.getAttribute("colorSchemeIndex");
		if (s.length() > 0)
			colorSchemeIdx = Integer.parseInt(s);

		/*
		 * This may be put back in later
		 * s = xml.getAttribute("maximaThreshold");
		 * if (s.length() > 0)
		 * maximaThreshold = Integer.parseInt(s);
		 * 
		 * s = xml.getAttribute("maximaPrecision");
		 * if (s.length() > 0)
		 * maximaPrecision = Integer.parseInt(s);
		 * 
		 * s = xml.getAttribute("maximaExpectedDiameter");
		 * if (s.length() > 0)
		 * maximaExpectedDiameter = Double.parseDouble(s);
		 */

		s = xml.getAttribute("latticeExpectedSpacing");
		if (s.length() > 0)
			expectedLatticeSpacing = Double.parseDouble(s);

		s = xml.getAttribute("latticeSpacingUncertainty");
		if (s.length() > 0)
			spacingUncertainty = Double.parseDouble(s);

		s = xml.getAttribute("latticeAngle");
		if (s.length() > 0)
			latticeAngle = Double.parseDouble(s);

		s = xml.getAttribute("detectionContrast");
		if (s.length() > 0)
			detectionContrast = Double.parseDouble(s);

		s = xml.getAttribute("predictionThreshold");
		if (s.length() > 0)
			predictionThreshold = Double.parseDouble(s);

		/*
		 * s = xml.getAttribute("sampleBias");
		 * if (s.length() > 0)
		 * bias = Double.parseDouble(s);
		 * 
		 * s = xml.getAttribute("current");
		 * if (s.length() > 0)
		 * current = Double.parseDouble(s);
		 */

		if (img == null)
			return;

		if (currentImageData != null) {
			// setImageTo(currentImageData);
			setImageChannel(imageChannel);
		}
	}

	public Element getAsXML() {
		Element e = super.getAsXML();

		e.setAttribute("maxZFraction", Float.toString(maxZFraction));
		e.setAttribute("minZFraction", Float.toString(minZFraction));
		e.setAttribute("planeSubtract", Boolean.toString(planeSubtract));
		e.setAttribute("lineByLineFlatten", Boolean.toString(lineByLineFlatten));
		e.setAttribute("imageChannel", imageChannel);
		e.setAttribute("colorSchemeIndex", Integer.toString(colorSchemeIdx));
		// e.setAttribute("maximaThreshold", Integer.toString(maximaThreshold));
		// e.setAttribute("maximaPrecision", Integer.toString(maximaPrecision));
		// e.setAttribute("maximaExpectedDiameter",
		// Double.toString(maximaExpectedDiameter));
		e.setAttribute("latticeExpectedSpacing", Double.toString(expectedLatticeSpacing));
		e.setAttribute("latticeSpacingUncertainty", Double.toString(spacingUncertainty));
		e.setAttribute("latticeAngle", Double.toString(latticeAngle));
		e.setAttribute("detectionContrast", Double.toString(detectionContrast));
		e.setAttribute("predictionThreshold", Double.toString(predictionThreshold));
		e.setAttribute("sampleBias", Double.toString(bias));
		e.setAttribute("current", Double.toString(current));
		return e;
	}

	public static float median(float[] vals) {
		float m = 0;

		int idx = (int) Math.floor((double) vals.length / 2.);
		Arrays.sort(vals);
		if (vals.length % 2 == 0) {
			m = (vals[idx - 1] + vals[idx]) / 2;
		} else {
			m = vals[idx];
		}

		return m;
	}

	public void nextColorScheme() {
		colorSchemeIdx++;
		if (colorSchemeIdx >= BufferedSTMImage.colorSchemes.size())
			colorSchemeIdx = 0;

		setImageTo(currentImageData);
	}

	public void locateMaxima() {
		if (replaceGroup != null) {
			SampleNavigator.setSelectedLayer(replaceGroup);
			SampleNavigator.selectedLayer.getChildren().removeAll(SampleNavigator.selectedLayer.getChildren());
		} else {
			GroupLayer maximaLayer = new GroupLayer();
			maximaLayer.name = "locateMaxima";
			SampleNavigator.selectedLayer.getChildren().add(maximaLayer);
			SampleNavigator.selectedLayer = maximaLayer;
			replaceGroup = maximaLayer;
		}
		SampleNavigator.refreshAttributeEditor();
		try {
			Thread t = new Thread(new Runnable() {
				public void run() {
					List<Double[]> maximaList = new ArrayList<Double[]>();
					for (int y = 0; y < bImg.getHeight(); y += maximaPrecision) {
						for (int x = 0; x < bImg.getWidth(); x += maximaPrecision) {
							int pixel = bImg.getRGB(x, y);
							Color c = new Color(pixel);
							if (c.getRed() + c.getGreen() + c.getBlue() > maximaThreshold) {
								Double[] tempList = { (double) x, (double) y };
								maximaList.add(tempList);
							}
						}
					}
					List<Double[]> skipList = new ArrayList<Double[]>();
					List<Double[]> addList = new ArrayList<Double[]>();
					for (Double[] thisPoint : maximaList) {
						if (!skipList.contains(thisPoint)) {
							List<Double[]> closeList = new ArrayList<Double[]>();
							closeList.add(thisPoint);
							for (Double[] thatPoint : maximaList) {
								if (Math.sqrt(Math.pow(thisPoint[0] - thatPoint[0], 2)
										+ Math.pow(thisPoint[1] - thatPoint[1], 2)) < maximaExpectedDiameter) {
									closeList.add(thatPoint);
								}
							}
							double xSum = 0;
							double ySum = 0;
							for (Double[] closePoint : closeList) {
								skipList.add(closePoint);
								xSum += closePoint[0];
								ySum += closePoint[1];
							}
							Double[] averagePoint = { xSum / closeList.size(), ySum / closeList.size() };
							addList.add(averagePoint);
						}
					}
					for (Double[] removePoint : skipList) {
						maximaList.remove(removePoint);
					}
					for (Double[] addPoint : addList) {
						maximaList.add(addPoint);
					}
					for (Double[] addPositioner : maximaList) {
						final double xn = (((double) addPositioner[0]) - (((double) bImg.getWidth()) / 2))
								/ bImg.getWidth();
						final double yn = (((double) addPositioner[1]) - (((double) bImg.getHeight()) / 2))
								/ bImg.getHeight();
						Platform.runLater(new Runnable() {
							public void run() {
								SampleNavigator.addPositioner(xn, yn);
							}
						});
					}
				}
			});
			t.start();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	public void altLocateLattice() {
		// currentImageData
		if (currentImageData == null)
			return;

		int w = currentImageData[0].length;
		int h = currentImageData.length;

		if (h != w)
			return;

		// pad the image data with 0's to have dimensions of power of 2
		double exp = Math.ceil(Math.log(h) / Math.log(2));
		int N = (int) Math.pow(2, exp);
		System.out.println("w: " + w + "    N: " + N);

		double scaleR = scale.getMxx();
		double scaleN = scaleR * (double) (N - 1) / (double) (w - 1);

		float[][] mod = new float[N][N];
		float[][] paddedData = new float[N][N];
		for (int yIdx = 0; yIdx < N; yIdx++) {
			for (int xIdx = 0; xIdx < N; xIdx++) {
				if ((xIdx < w) && (yIdx < h))
					paddedData[yIdx][xIdx] = currentImageData[yIdx][xIdx];
				else
					paddedData[yIdx][xIdx] = 0;
			}
		}

		// flatten the image
		// now do the plane subtract
		float dzdxAve = 0;
		float dzdyAve = 0;
		for (int yIdx = 0; yIdx < pixHeight; yIdx++) {
			dzdxAve += paddedData[pixWidth - 1][yIdx] - paddedData[0][yIdx];
		}

		for (int xIdx = 0; xIdx < pixWidth; xIdx++) {
			dzdyAve += paddedData[xIdx][pixHeight - 1] - paddedData[xIdx][0];
		}

		dzdxAve /= pixWidth * pixHeight;
		dzdyAve /= pixWidth * pixHeight;

		// System.out.println("slopes: " + dzdxAve + " " + dzdyAve);

		for (int yIdx = 0; yIdx < pixHeight; yIdx++) {
			for (int xIdx = 0; xIdx < pixWidth; xIdx++) {
				paddedData[xIdx][yIdx] -= (float) (dzdxAve * xIdx + dzdyAve * yIdx);
			}
		}

		float min = 99999;
		float max = 0;
		for (int yIdx = 0; yIdx < w; yIdx++) {
			for (int xIdx = 0; xIdx < w; xIdx++) {
				if (paddedData[yIdx][xIdx] < min)
					min = paddedData[yIdx][xIdx];
				if (paddedData[yIdx][xIdx] > max)
					max = paddedData[yIdx][xIdx];
			}
		}

		float ave = 0;
		for (int yIdx = 0; yIdx < w; yIdx++) {
			for (int xIdx = 0; xIdx < w; xIdx++) {
				paddedData[yIdx][xIdx] = (paddedData[yIdx][xIdx] - min) / (max - min);
				ave += paddedData[yIdx][xIdx];
			}
		}

		ave /= (w * w);

		for (int yIdx = 0; yIdx < w; yIdx++) {
			for (int xIdx = 0; xIdx < w; xIdx++) {
				paddedData[yIdx][xIdx] -= ave;
			}
		}

		// apply a Hann window to the data
		for (int yIdx = 0; yIdx < w; yIdx++) {
			for (int xIdx = 0; xIdx < w; xIdx++) {
				float x = (w - 1) / 2 - xIdx;
				float y = (w - 1) / 2 - yIdx;
				float r = (float) Math.sqrt(x * x + y * y);
				float s = (float) Math.sin(Math.PI * r / w);
				s *= s;

				if (r >= (w - 1) / 2)
					s = 1;

				paddedData[yIdx][xIdx] = (1 - s) * (paddedData[yIdx][xIdx]);// -min)/(max-min);
			}
		}

		FFT2D.fft2Dmod(paddedData, mod);
		// mod = paddedData;

		min = 99999;
		max = 0;
		for (int yIdx = 0; yIdx < N; yIdx++) {
			for (int xIdx = 0; xIdx < N; xIdx++) {
				if (mod[yIdx][xIdx] < min)
					min = mod[yIdx][xIdx];
				if (mod[yIdx][xIdx] > max)
					max = mod[yIdx][xIdx];
			}
		}
		System.out.println("fft2D min: " + min + "    max: " + max);
		float s = 1f / (max - min);
		// normalize the data
		float[][] shiftMod = new float[N][N];
		for (int yIdx = 0; yIdx < N; yIdx++) {
			for (int xIdx = 0; xIdx < N; xIdx++) {
				// float val = (float)Math.log( 1.0 + s*(mod[(yIdx+N/2)%N][(xIdx+N/2)%N] - min)
				// );
				float val = s * (mod[(yIdx + N / 2) % N][(xIdx + N / 2) % N] - min);
				// val = (float)Math.log(1+val);
				val = (float) Math.pow(Math.log(1 + val), 0.2);
				shiftMod[yIdx][xIdx] = val;// s*(mod[(yIdx+N/2)%N][(xIdx+N/2)%N] - min);
			}
		}

		// setImageTo(shiftMod);

		BufferedSTMImage bImg = new BufferedSTMImage(shiftMod);
		bImg.colorSchemeIdx = this.colorSchemeIdx;
		bImg.minZFraction = this.minZFraction;
		bImg.maxZFraction = this.maxZFraction;
		bImg.processData = false;
		bImg.draw();

		try {
			File imageOut = new File("fftImage.png");
			ImageIO.write(bImg, "png", imageOut);
		} catch (Exception exc) {
			exc.printStackTrace();
		}

		// setImageTo(paddedData);

		// find kx,ky coordinates of maximum
		double maxK = scaleN / (expectedLatticeSpacing - spacingUncertainty);
		double minK = scaleN / (expectedLatticeSpacing + spacingUncertainty);
		double peak = 0;
		double kXpeak = 0;
		double kYpeak = 0;

		for (int kX = 0; kX < N; kX++) {
			for (int kY = 0; kY < N; kY++) {
				double x = (double) (N - 1) / 2 - (double) kX;
				double y = (double) (N - 1) / 2 - (double) kY;

				double k = Math.sqrt((double) x * x + (double) y * y);
				if ((k < maxK) && (k > minK)) {
					if (shiftMod[kX][kY] > peak) {
						peak = shiftMod[kX][kY];
						kXpeak = x;
						kYpeak = y;
					}
				}
			}
		}

		double kPeak = Math.sqrt((double) kXpeak * kXpeak + (double) kYpeak * kYpeak);
		double lambda = scaleN / kPeak;
		double theta = Math.atan2(kYpeak, kXpeak);

		System.out.println("lambda: " + lambda + "    theta: " + theta * 180 / Math.PI);

		// put in the lattice
		/*
		 * if (replaceLattice == null)
		 * {
		 * latticeLayer = new GroupLayer();
		 * latticeLayer.name= "latticeLayer";
		 * latticeLayer.setOpacity(0.2);
		 * SampleNavigator.selectedLayer.getChildren().add(latticeLayer);
		 * replaceLattice = latticeLayer;
		 * }
		 * else
		 * {
		 * latticeLayer = replaceLattice;
		 * latticeLayer.getChildren().removeAll(latticeLayer.getChildren());
		 * }
		 * SampleNavigator.addSegment(intervalF, angleF, latticeLayer,
		 * latticeStartingXF, latticeStartingYF);
		 * if (angleF > 0)
		 * {
		 * SampleNavigator.addSegment(intervalF, angleF-(Math.PI/2), latticeLayer,
		 * latticeStartingXF, latticeStartingYF);
		 * }
		 */

		NavigationLayer latticeLayer = getOrMakeGroup("latticeLayer");
		latticeLayer.getChildren().clear();
		SampleNavigator.addSegment(lambda / scaleR, theta, latticeLayer, 0, 0);
	}

	public void locateLattice() {
		try {
			Thread t = new Thread(new Runnable() {
				// @SuppressWarnings("deprecation")
				public void run() {
					int width = bImg.getWidth();
					int height = bImg.getHeight();

					double heightWidthNM = scale.getMxx();
					if (width != height || scale.getMxx() != scale.getMyy()) {
						System.out.println("Failed: image not square");
						return;
					}

					// Get positioners

					ArrayList<Node> itemList = new ArrayList<Node>();

					for (Node item : SampleNavigator.selectedLayer.getChildren()) {
						if (item instanceof Positioner) {
							itemList.add(item);
						}
					}

					double latticeStartingX = 0;
					double latticeStartingY = 0;

					if (itemList.size() >= 2) {
						double xDist = Math.abs(itemList.get(itemList.size() - 1).getTranslateX()
								- itemList.get(itemList.size() - 2).getTranslateX());
						double yDist = Math.abs(itemList.get(itemList.size() - 1).getTranslateY()
								- itemList.get(itemList.size() - 2).getTranslateY());
						expectedLatticeSpacing = heightWidthNM * Math.sqrt(Math.pow(xDist, 2) + Math.pow(yDist, 2));
						latticeStartingX = itemList.get(itemList.size() - 2).getTranslateX();
						latticeStartingY = itemList.get(itemList.size() - 2).getTranslateY();
					} else if (itemList.size() >= 1) {
						latticeStartingX = itemList.get(itemList.size() - 1).getTranslateX();
						latticeStartingY = itemList.get(itemList.size() - 1).getTranslateY();
					}

					// Pads image to a height and width that is a power of 2

					int n = (int) Math.pow(2, ((int) (Math.log(width) / Math.log(2)) + 2));
					BufferedImage transformedImage = new BufferedImage(n, n, 2);
					Complex[][] pixel = new Complex[n][n];
					for (int i = 0; i < n; i++) {
						for (int j = 0; j < n; j++) {
							try {
								pixel[i][j] = new Complex(bImg.getRGB(i, j) << 24 >> 24 & 0xff, 0);
							} catch (Exception e) {
								pixel[i][j] = new Complex(0, 0);
							}
						}
					}

					// Performs transform

					Complex[][] answers = new Complex[n][n];
					FastFourierTransformer f = new FastFourierTransformer(DftNormalization.STANDARD);
					answers = (Complex[][]) f.mdfft(pixel, TransformType.FORWARD);

					// Converts array to image

					for (int i = 0; i < n; i++) {
						for (int j = 0; j < n; j++) {
							double brightness = Math.sqrt(
									Math.pow(answers[i][j].getReal(), 2) + Math.pow(answers[i][j].getImaginary(), 2));
							brightness = Math.pow(Math.log(1 + brightness), 2.3);
							if (brightness > 255) {
								brightness = 255;
							}
							if (brightness < 0) {
								brightness = 0;
							}
							int brightnessInt = (int) brightness;
							transformedImage.setRGB((i + n / 2) % n, (j + n / 2) % n,
									255 << 24 | brightnessInt << 16 | brightnessInt << 8 | brightnessInt);
						}
					}

					try {
						File imageOut = new File("fftImage.png");
						ImageIO.write(transformedImage, "png", imageOut);
					} catch (Exception exc) {
						exc.printStackTrace();
					}

					// Gets most likely angle

					ArrayList<Double[]> peaks = new ArrayList<Double[]>();
					double lowerNM = expectedLatticeSpacing + spacingUncertainty;
					double upperNM = expectedLatticeSpacing - spacingUncertainty;
					double expectedRadius = (n * heightWidthNM) / (width * expectedLatticeSpacing);
					double lowerRadius = (n * heightWidthNM) / (width * lowerNM);
					double upperRadius = (n * heightWidthNM) / (width * upperNM);

					double max = 0;
					double sum = 0;
					int count = 0;
					Double[] maxAngle = { 0.0, 0.0 };
					ArrayList<Double[]> angleList = new ArrayList<Double[]>();
					int count2 = 0;
					double likelihoodAverage = 0;

					for (double theta = 0; theta < Math.PI * 2; theta += 1 / expectedRadius) {
						max = 0;
						sum = 0;
						count = 0;
						for (double radius = lowerRadius; radius < upperRadius; radius++) {
							int x = (int) ((n / 2) + (radius * Math.cos(theta)));
							int y = (int) ((n / 2) + (-radius * Math.sin(theta)));
							int thisPixel = 0;
							try {
								thisPixel = transformedImage.getRGB(x, y);
							} catch (Exception ex) {
								System.out.println(
										"Radius out of bounds (increase expected spacing or decrease spacing uncertainty)");
								return;
							}
							Color c = new Color(thisPixel);
							double thisBright = c.getRed() + c.getBlue() + c.getGreen();
							sum += thisBright;
							if (thisBright > max) {
								max = thisBright;
							}
							count++;
							if (c.getRed() < 235 && c.getBlue() > 10 && c.getGreen() > 10) {
								transformedImage.setRGB(x, y,
										(new Color(c.getRed() + 20, c.getBlue() - 10, c.getGreen() - 10)).getRGB());
							}
						}
						sum /= count;

						count2++;
						likelihoodAverage += (max / sum);

						Double[] toAdd = { theta, max / sum };
						angleList.add(toAdd);

						if ((max / sum) > maxAngle[1]) {
							maxAngle = toAdd;
						}
					}

					likelihoodAverage /= count2;

					double sumSquareDiff = 0;
					for (Double[] item : angleList) {
						sumSquareDiff += Math.pow(item[1] - likelihoodAverage, 2);
					}
					double stdev = Math.sqrt(sumSquareDiff / angleList.size());

					// See if most likely angle is likely at all

					double sum2;
					double max2;
					int count3;
					ArrayList<Double[]> brightnessAtAngle = new ArrayList<Double[]>();

					for (double theta = maxAngle[0] % (Math.PI / 2); theta < Math.PI * 2; theta += Math.PI / 2) {
						count3 = 0;
						sum2 = 0;
						max2 = 0;
						for (double radius = lowerRadius; radius < upperRadius; radius++) {
							int x = (int) ((n / 2) + (radius * Math.cos(theta)));
							int y = (int) ((n / 2) + (-radius * Math.sin(theta)));
							int thisPixel = 0;
							try {
								thisPixel = transformedImage.getRGB(x, y);
							} catch (Exception ex) {
								System.out.println(
										"Radius out of bounds (increase expected spacing or decrease spacing uncertainty)");
								return;
							}
							Color c = new Color(thisPixel);
							double thisBright = c.getRed() + c.getBlue() + c.getGreen();
							sum2 += thisBright;
							if (thisBright > max2) {
								max2 = thisBright;
							}
							count3++;
						}
						sum2 /= count3;
						Double[] toAdd = { theta, ((max2 / sum2) - likelihoodAverage) / stdev };
						brightnessAtAngle.add(toAdd);

					}

					ArrayList<Double[]> brightStandardizedAngles = new ArrayList<Double[]>();
					ArrayList<Double[]> darkStandardizedAngles = new ArrayList<Double[]>();
					for (Double[] item : brightnessAtAngle) {
						if (item[1] > 3.5)// threshold z score for significantly bright spot
						{
							brightStandardizedAngles.add(item);
						} else if (item[1] < 2)// threshold z score for significantly dark spot
						{
							darkStandardizedAngles.add(item);
						}
					}
					if (darkStandardizedAngles.size() > 0 && brightStandardizedAngles.size() < 2) {
						maxAngle[0] = 0.0;
					}

					// Gets spacing interval

					for (double theta = maxAngle[0] % (Math.PI / 2); theta < Math.PI * 2; theta += Math.PI / 2) {
						for (double radius = lowerRadius; radius < upperRadius; radius++) {
							int x = (int) Math.round((n / 2) + (radius * Math.cos(theta)));
							int y = (int) Math.round((n / 2) + (-radius * Math.sin(theta)));
							int thisPixel = 0;
							try {
								thisPixel = transformedImage.getRGB(x, y);
								transformedImage.setRGB(x, y, Color.BLUE.getRGB());
							} catch (Exception ex) {
								System.out.println(
										"Radius out of bounds (increase expected spacing or decrease spacing uncertainty)");
								return;
							}
							Color c = new Color(thisPixel);
							double thisBright = c.getRed() + c.getBlue() + c.getGreen();
							Double[] toAdd = { (double) x, (double) y, thisBright };
							if (!peaks.contains(toAdd)) {
								peaks.add(toAdd);
							}
						}
					}

					double angle = maxAngle[0];
					double sumDist = 0;
					double interval = 0;
					int halfWidth = n / 2;

					Double[] type1Max = { 0.0, 0.0, 0.0 };
					Double[] type2Max = { 0.0, 0.0, 0.0 };
					Double[] type3Max = { 0.0, 0.0, 0.0 };
					Double[] type4Max = { 0.0, 0.0, 0.0 };
					for (Double[] item : peaks) {
						if ((item[0] >= n / 2) && (item[1] > n / 2)) {
							if (item[2] > type1Max[2]) {
								type1Max = item;
							}
						} else if ((item[0] <= n / 2) && (item[1] < n / 2)) {
							if (item[2] > type2Max[2]) {
								type2Max = item;
							}
						} else if ((item[0] > n / 2) && (item[1] <= n / 2)) {
							if (item[2] > type3Max[2]) {
								type3Max = item;
							}
						} else if ((item[0] < n / 2) && (item[1] >= n / 2)) {
							if (item[2] > type4Max[2]) {
								type4Max = item;
							}
						}
					}

					transformedImage.setRGB((int) Math.round(type1Max[0]), (int) Math.round(type1Max[1]),
							Color.GREEN.getRGB());
					transformedImage.setRGB((int) Math.round(type2Max[0]), (int) Math.round(type2Max[1]),
							Color.GREEN.getRGB());
					transformedImage.setRGB((int) Math.round(type3Max[0]), (int) Math.round(type3Max[1]),
							Color.GREEN.getRGB());
					transformedImage.setRGB((int) Math.round(type4Max[0]), (int) Math.round(type4Max[1]),
							Color.GREEN.getRGB());

					if (type1Max[0] + type2Max[0] - n > 5 || type3Max[1] + type4Max[1] - n > 5) {
						System.out.println("Warning: transform is asymmetrical, try reducing spacing uncertainty");
					}
					Double[][] peaksToAdd = { type1Max, type2Max, type3Max, type4Max };
					for (Double[] item : peaksToAdd) {
						sumDist += Math.sqrt(Math.pow(item[0] - halfWidth, 2) + Math.pow(item[1] - halfWidth, 2));
					}
					sumDist /= 4;
					sumDist = width * (sumDist / n);
					interval = 1 / sumDist;

					// Construct lattice

					final double latticeStartingXF = latticeStartingX;
					final double latticeStartingYF = latticeStartingY;
					final double intervalF = interval;
					final double angleF = angle;
					Platform.runLater(new Runnable() {
						public void run() {
							PrintStream printStream = System.out;
							System.setOut(new PrintStream(new OutputStream() {
								public void write(int b) throws IOException {
								}
							}));
							// prevents the print statements when the line segments are created
							GroupLayer latticeLayer;
							if (replaceLattice == null) {
								latticeLayer = new GroupLayer();
								latticeLayer.name = "latticeLayer";
								latticeLayer.setOpacity(0.2);
								SampleNavigator.selectedLayer.getChildren().add(latticeLayer);
								replaceLattice = latticeLayer;
							} else {
								latticeLayer = replaceLattice;
								latticeLayer.getChildren().removeAll(latticeLayer.getChildren());
							}
							SampleNavigator.addSegment(intervalF, angleF, latticeLayer, latticeStartingXF,
									latticeStartingYF);
							if (angleF > 0) {
								SampleNavigator.addSegment(intervalF, angleF - (Math.PI / 2), latticeLayer,
										latticeStartingXF, latticeStartingYF);
							} else {
								SampleNavigator.addSegment(intervalF, angleF + (Math.PI / 2), latticeLayer,
										latticeStartingXF, latticeStartingYF);
							}
							SampleNavigator.refreshTreeEditor();
							System.setOut(printStream);
							System.out.println("Interval: " + heightWidthNM * intervalF);
							System.out.println("Angle: " + angleF);
						}
					});
				}
			});
			t.start();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void addExample() {
		SampleNavigator.addExample(this, 0, 0);

	}

	public void clearExamples() {
		GroupLayer exampleGroup = getOrMakeGroup("examples");
		// <T> Vector<T> getChildrenOfType(Class<T> type)
		Vector<ExampleLayer> examples = exampleGroup.getChildrenOfType(ExampleLayer.class);
		for (ExampleLayer example : examples) {
			if (SampleNavigator.mlController != null) {
				SampleNavigator.mlController.examples.remove(example);
				if (SampleNavigator.mlController.currentExample == example)
					SampleNavigator.mlController.currentExample = null;
			}
			exampleGroup.getChildren().remove(example);
		}

		SampleNavigator.refreshTreeEditor();
	}

	public void deleteExample(ExampleLayer example) {
		if (SampleNavigator.mlController != null) {
			SampleNavigator.mlController.examples.remove(example);
			if (SampleNavigator.mlController.currentExample == example)
				SampleNavigator.mlController.currentExample = null;
		}

		// GroupLayer exampleGroup = getOrMakeGroup("examples");
		GroupLayer exampleGroup = (GroupLayer) example.getParent();
		exampleGroup.getChildren().remove(example);

		SampleNavigator.refreshTreeEditor();
	}

	public void checkTipQuality() {
		// send the image to python to analyze it
		String imgNameString = new String(imgName);
		imgNameString = imgNameString.replaceFirst("file:", "file:" + SampleNavigator.relativeDirectory);
		String fullFileName = imgNameString.replaceFirst("file:", "");
		File f = new File(fullFileName);
		String pathToImage = f.getAbsolutePath();

		JSONObject jObj = new JSONObject();

		jObj.put("command", "checkTipQuality");

		jObj.put("scan_path", pathToImage);

		JSONObject options = new JSONObject();
		options.put("contrast", Double.valueOf(detectionContrast));
		options.put("rotation", Double.valueOf(latticeAngle));
		jObj.put("detector_options", options);

		options = new JSONObject();
		options.put("direction", Integer.valueOf(0));

		JSONArray slopes = new JSONArray();
		slopes.add(Double.valueOf(dzdx));
		slopes.add(Double.valueOf(dzdy));
		options.put("plane_slopes", slopes);
		jObj.put("matrix_options", options);

		System.out.println(jObj);
		String result = ABDPythonAPIClient.command(jObj.toString());
		// System.out.println("tip check result: " + result);

		// read the result
		try {
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(result);
			JSONObject rObj = (JSONObject) obj;

			Object s = rObj.get("sharp");
			if (s == null)
				return;
			int sharp = ((Long) s).intValue();

			s = rObj.get("dull");
			int dull = ((Long) s).intValue();

			s = rObj.get("total");
			int total = ((Long) s).intValue();

			JSONObject roi = (JSONObject) rObj.get("roi_data");

			JSONObject constants = (JSONObject) roi.get("constants");
			s = constants.get("nm_size");
			double nmSize = ((Double) s).doubleValue();

			s = constants.get("pixel_size");
			int pxSize = ((Long) s).intValue();

			double imgPxSize = (double) getRasterData().length;
			double origin = imgPxSize / 2;
			double scaleSize = (double) pxSize / imgPxSize;

			JSONArray locations = (JSONArray) roi.get("locations");

			GroupLayer detectionGroup = getOrMakeGroup("detections");
			detectionGroup.getChildren().clear();
			detectionGroup.rotation.setAngle(latticeAngle);

			for (int i = 0; i < locations.size(); i++) {
				// System.out.println( locations.get(i).getClass() );
				JSONObject loc = (JSONObject) locations.get(i);

				double x = (double) ((Long) loc.get("x")).intValue();
				double y = (double) ((Long) loc.get("y")).intValue();
				double prediction = ((Double) loc.get("prediction")).doubleValue();

				double xScaled = (x - origin) / imgPxSize + scaleSize / 2;
				double yScaled = (y - origin) / imgPxSize + scaleSize / 2;

				System.out.println(x + "  " + y + "    " + prediction);
				SampleNavigator.addDetection(this, xScaled, yScaled, scaleSize, scaleSize, prediction,
						predictionThreshold);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public double[][] getRawImageData() {
		// return getRasterData();
		if (currentImageData == null) {
			System.out.println("no current image yet");
			return null;
		}

		double[][] data = new double[currentImageData.length][currentImageData[0].length];
		for (int i = 0; i < data.length; i++)
			for (int j = 0; j < data[0].length; j++)
				data[i][j] = (double) currentImageData[i][j];

		return data;
	}
}
