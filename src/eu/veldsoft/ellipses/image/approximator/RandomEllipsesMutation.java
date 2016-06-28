package eu.veldsoft.ellipses.image.approximator;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.math3.genetics.Chromosome;
import org.apache.commons.math3.genetics.MutationPolicy;

class RandomEllipsesMutation implements MutationPolicy {
	private BufferedImage image = null;
	private Vector<Color> colors = null;

	public RandomEllipsesMutation(BufferedImage image, Vector<Color> colors) {
		this.image = image;
		this.colors = colors;
	}

	@Override
	public Chromosome mutate(Chromosome original) {
		if (!(original instanceof EllipseListChromosome)) {
			throw new IllegalArgumentException();
		}

		double factor = Util.PRNG.nextDouble();

		List<Ellipse> values = new ArrayList<Ellipse>();
		for (Ellipse value : ((EllipseListChromosome) original).getEllipses()) {
			int dx = (int) (value.width * factor);
			int dy = (int) (value.height * factor);
			double theta = 2 * Math.PI * Util.PRNG.nextDouble();

			Ellipse ellipse = new Ellipse(value);

			/*
			 * Mutate color in some cases by taking color of other ellipse.
			 */
			if (Util.PRNG.nextDouble() < factor) {
				ellipse.color = ((EllipseListChromosome) original).getRandomElement().color;
			}

			/*
			 * Mutate positions.
			 */
			if (Util.PRNG.nextBoolean() == true) {
				ellipse.x1 -= dx;
			} else {
				ellipse.x1 += dx;
			}
			if (Util.PRNG.nextBoolean() == true) {
				ellipse.y1 -= dy;
			} else {
				ellipse.y1 += dy;
			}

			/*
			 * Mutate rotation.
			 */
			ellipse.setup((int) ((ellipse.x1 + ellipse.x2) / 2.0), (int) ((ellipse.y1 + ellipse.y2) / 2.0), theta);

			// TODO Ellipse should not be outside of the image.
			if (ellipse.x1 < 0 || ellipse.y1 < 0 || ellipse.x2 < 0 || ellipse.y2 < 0 || ellipse.x1 >= image.getWidth()
					|| ellipse.y1 >= image.getHeight() || ellipse.x2 >= image.getWidth()
					|| ellipse.y2 >= image.getHeight()) {
				ellipse.setup((int) (image.getWidth() / 2.0), (int) (image.getHeight() / 2.0), theta);
			}

			values.add(ellipse);
		}

		return new EllipseListChromosome(values, image, colors);
	}
}