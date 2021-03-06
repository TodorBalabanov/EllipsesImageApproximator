package eu.veldsoft.ellipses.image.approximator;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import javax.imageio.ImageIO;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.commons.math3.genetics.Chromosome;
import org.apache.commons.math3.genetics.ElitisticListPopulation;
import org.apache.commons.math3.genetics.Population;

import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;

class Util {
	/** A pseudo-random number generator instance. */
	private static final Random PRNG = new Random();

	private static ColorComparator euclidean = new EuclideanColorComparator();

	static int DEFAULT_THREAD_POOL_SIZE = 1;

	static String log = "";

	private static Ellipse findBetter(int x, int y, BufferedImage image,
			double theta, Color color) {
		int width = Math.max(Ellipse.WIDTH(), Ellipse.HEIGHT());
		int height = Math.max(Ellipse.WIDTH(), Ellipse.HEIGHT());
		int left = x - width / 2;
		int top = y - height / 2;

		if (left < 0) {
			left = 0;
		}

		if (top < 0) {
			top = 0;
		}

		if (left + width >= image.getWidth()) {
			left = image.getWidth() - width;
		}

		if (top + height >= image.getHeight()) {
			top = image.getHeight() - height;
		}

		final ImageComparator comparator = new EuclideanImageComparator();
		final BufferedImage squeare = image.getSubimage(left, top, width,
				height);

		BufferedImage attempt = new BufferedImage(squeare.getWidth(),
				squeare.getHeight(), BufferedImage.TYPE_INT_ARGB);
		List<Ellipse> list = new ArrayList<Ellipse>();
		list.add(new Ellipse(width / 2, height / 2, theta, color));

		attempt = drawEllipses(attempt, list);

		double better = Double.MAX_VALUE;
		double distance = comparator.distance(squeare, attempt);

		while (distance < better) {
			better = distance;

			theta = 2.0D * Math.PI * PRNG.nextDouble();

			list.clear();
			list.add(new Ellipse(width / 2, height / 2, theta, color));
			attempt = new BufferedImage(squeare.getWidth(), squeare.getHeight(),
					BufferedImage.TYPE_INT_ARGB);

			attempt = drawEllipses(attempt, list);

			distance = comparator.distance(squeare, attempt);
		}

		return new Ellipse(x, y, theta, color);
	}

	static private void writePngSolution(int width, int height,
			List<Ellipse> list, double fitness, String file) {
		ByteArrayOutputStream os = null;
		try {
			ImageIO.write(
					Util.drawEllipses(new BufferedImage(width, height,
							BufferedImage.TYPE_INT_ARGB), list),
					"png", os = new ByteArrayOutputStream());
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		PngReader reader = new PngReader(
				new ByteArrayInputStream(os.toByteArray()));
		PngWriter writer = new PngWriter(new File(file), reader.imgInfo, true);

		writer.copyChunksFrom(reader.getChunksList(),
				ChunkCopyBehaviour.COPY_ALL_SAFE);
		writer.getMetadata().setText("Ellipses", "" + list.size());
		writer.getMetadata().setText("Fitness", "" + fitness);

		for (int row = 0; row < reader.imgInfo.rows; row++) {
			writer.writeRow(reader.readRow());
		}

		reader.end();
		writer.end();
	}

	static private void writeSvgSolution(int width, int height,
			List<Ellipse> list, double fitness, String file) {
		SVGGraphics2D graphics = new SVGGraphics2D(
				GenericDOMImplementation.getDOMImplementation().createDocument(
						"http://www.w3.org/2000/svg", "svg", null));

		graphics.setSVGCanvasSize(new Dimension(width, height));

		for (Ellipse shape : list) {
			graphics.setColor(shape.color);
			graphics.setBackground(shape.color);

			Shape ellipse = new Ellipse2D.Double(-Ellipse.WIDTH() / 2D,
					-Ellipse.HEIGHT() / 2D, Ellipse.WIDTH(), Ellipse.HEIGHT());

			AffineTransform rotator = new AffineTransform();
			rotator.rotate(shape.theta);
			ellipse = rotator.createTransformedShape(ellipse);

			AffineTransform translator = new AffineTransform();
			translator.translate(
					-ellipse.getBounds().getX() + Math.min(shape.x1, shape.x2),
					-ellipse.getBounds().getY() + Math.min(shape.y1, shape.y2));
			ellipse = translator.createTransformedShape(ellipse);

			graphics.fill(ellipse);
			graphics.draw(ellipse);
		}

		try {
			Writer out = new BufferedWriter(new FileWriter(new File(file)));
			graphics.stream(out, false);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void writeSolution(int width, int height, List<Ellipse> list,
			double fitness, String file) {
		if (file.indexOf(".png") != -1) {
			writePngSolution(width, height, list, fitness, file);
		}

		if (file.indexOf(".svg") != -1) {
			writeSvgSolution(width, height, list, fitness, file);
		}
	}

	static Color closestColor(int rgb, Vector<Color> colors) {
		if (colors.size() <= 0) {
			throw new RuntimeException("List of colors can not be emtpy!");
		}

		Color candidate, bestColor = colors.get(0);
		double distance, bestDistance = euclidean.distance(rgb & 0xFFFFFF,
				bestColor.getRGB() & 0xFFFFFF);

		for (int i = colors.size() - 1; i > 0; i--) {
			candidate = colors.get(i);
			distance = euclidean.distance(rgb & 0xFFFFFF,
					candidate.getRGB() & 0xFFFFFF);

			if (distance < bestDistance) {
				bestColor = candidate;
				bestDistance = distance;
			}
		}

		return bestColor;
	}

	static double alphaLevel(BufferedImage image, Vector<Color> colors) {
		int pixels[] = image.getRGB(0, 0, image.getWidth(), image.getHeight(),
				null, 0, image.getWidth());

		int level = 0;
		for (int i = 0; i < pixels.length; i++) {
			if (pixels[i] == 0x01FFFFFF) {
				level++;
			}
		}

		return (double) level / (double) pixels.length;
	}

	static BufferedImage drawEllipses(BufferedImage image,
			List<Ellipse> ellipses) {
		// TODO Implement colors merge in overlapping ellipses.

		Graphics2D graphics = (Graphics2D) image.getGraphics();

		/* Fill with light background. */
		graphics.setColor(new Color(0xFF, 0xFF, 0xFF, 0x01));
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

		graphics.setStroke(Ellipse.STROKE);
		for (Ellipse ellipse : ellipses) {
			graphics.setColor(ellipse.color);
			graphics.draw(ellipse.line);
		}

		return image;
	}

	static Vector<Ellipse> randomApproximatedEllipses(BufferedImage image,
			Vector<Color> colors, boolean pixelClosestColor,
			boolean localSearch) {
		int length = (int) (PRNG.nextGaussian()
				* EllipseListChromosome.LENGTH_SD()
				+ EllipseListChromosome.LENGTH_MEAN());

		return new Vector<Ellipse>(randomRepresentation(image, colors,
				pixelClosestColor, localSearch, length));
	}

	static List<Ellipse> randomRepresentation(BufferedImage image,
			Vector<Color> colors, boolean pixelClosestColor,
			boolean localSearch, int length) {
		List<Ellipse> random = new ArrayList<Ellipse>();

		for (int i = 0, x, y; i < length; i++) {
			x = PRNG.nextInt(image.getWidth());
			y = PRNG.nextInt(image.getHeight());

			double theta = 2.0D * Math.PI * PRNG.nextDouble();

			Color color = colors.elementAt(PRNG.nextInt(colors.size()));
			if (pixelClosestColor == true) {
				color = closestColor(image.getRGB(x, y), colors);
			}

			/* Implement a local search for a better orientation angle. */
			if (localSearch == true) {
				random.add(findBetter(x, y, image, theta, color));
			} else {
				random.add(new Ellipse(x, y, theta, color));
			}
		}

		return random;
	}

	static Population randomInitialPopulation(BufferedImage image,
			Map<String, Integer> histogram, Vector<Color> colors,
			boolean pixelClosestColor, boolean localSearch, int populationSize,
			double elitismRate) {
		List<Chromosome> list = new LinkedList<Chromosome>();
		for (int i = 0; i < populationSize; i++) {
			int size = (int) (PRNG.nextGaussian()
					* EllipseListChromosome.LENGTH_SD()
					+ EllipseListChromosome.LENGTH_MEAN());

			list.add(
					new EllipseListChromosome(
							randomRepresentation(image, colors,
									pixelClosestColor, localSearch, size),
							image, histogram, colors));
		}
		return new ElitisticListPopulation(list, list.size(), elitismRate);
	}
}
