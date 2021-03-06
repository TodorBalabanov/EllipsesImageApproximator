package eu.veldsoft.ellipses.image.approximator;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import javax.imageio.ImageIO;

class Population1 {
	private static final Random PRNG = new Random();

	private static final ImageComparator IMAGE_COMPARATOR = new EuclideanImageComparator();

	private Chromosome1 first = null;
	private Chromosome1 second = null;
	private Chromosome1 offspring = null;
	private Chromosome1 result = null;
	private Vector<Chromosome1> chromosomes = new Vector<Chromosome1>();
	private BufferedImage image = null;
	private BufferedImage experimental = null;
	private boolean colorsEvolution = false;

	Chromosome1 best = null;

	public Population1(Population1 population) {
		super();

		this.first = null;
		this.second = null;
		this.offspring = null;
		this.result = null;
		this.image = population.image;
		this.experimental = null;
		this.best = population.best;
		this.chromosomes = new Vector<Chromosome1>();

		for (Chromosome1 c : population.chromosomes) {
			chromosomes.add(new Chromosome1(c));
		}
	}

	public Population1(int size, BufferedImage image, Vector<Color> colors,
			boolean pixelClosestColor, boolean localSearch,
			boolean colorsEvolution) {
		super();

		this.image = image;

		/*
		 * At least 4 chromosomes are needed in order the algorithm to work.
		 */
		if (size < 4) {
			size = 4;
		}

		/*
		 * Generate random population and evaluate chromosomes in it.
		 */
		for (int i = 0; i < size; i++) {
			offspring = new Chromosome1(colors,
					Util.randomApproximatedEllipses(image, colors,
							pixelClosestColor, localSearch),
					Double.MAX_VALUE);
			evaluate();
			chromosomes.addElement(offspring);
		}
		offspring = null;

		/*
		 * Find the best initial chromosome.
		 */
		best = chromosomes.firstElement();
		for (Chromosome1 c : chromosomes) {
			if (best.fittnes > c.fittnes) {
				best = c;
			}
		}

		this.colorsEvolution = colorsEvolution;
	}

	void select() {
		do {
			first = chromosomes.get(PRNG.nextInt(chromosomes.size()));
			second = chromosomes.get(PRNG.nextInt(chromosomes.size()));
			result = chromosomes.get(PRNG.nextInt(chromosomes.size()));

			/*
			 * Selection of better parents.
			 */
			double level = PRNG.nextDouble();
			if (level > 0.95) {
				if (result.fittnes > first.fittnes) {
					Chromosome1 buffer = result;
					result = first;
					first = buffer;
				}
				if (result.fittnes > second.fittnes) {
					Chromosome1 buffer = result;
					result = second;
					second = buffer;
				}
			} else if (level > 0.65) {
				if (first.fittnes > second.fittnes) {
					Chromosome1 buffer = first;
					first = second;
					second = buffer;
				}
				if (result.fittnes < first.fittnes) {
					Chromosome1 buffer = result;
					result = first;
					first = buffer;
				}
				if (result.fittnes > second.fittnes) {
					Chromosome1 buffer = result;
					result = second;
					second = buffer;
				}
			} else if (level > 0.00) {
				if (result.fittnes < first.fittnes) {
					Chromosome1 buffer = result;
					result = first;
					first = buffer;
				}
				if (result.fittnes < second.fittnes) {
					Chromosome1 buffer = result;
					result = second;
					second = buffer;
				}
			}
		} while (result == best || first == second || first == result
				|| second == result);
	}

	void crossover() {
		/*
		 * Crossover probability.
		 */
		if (PRNG.nextDouble() > 0.96) {
			do {
				offspring = chromosomes.get(PRNG.nextInt(chromosomes.size()));
			} while (offspring == best);
			return;
		}

		offspring = new Chromosome1(new Vector<Color>(), new Vector<Ellipse>(),
				Double.MAX_VALUE);

		for (int i = 0; i < first.colors.size()
				&& i < second.colors.size(); i++) {
			if (PRNG.nextBoolean()) {
				offspring.colors.add(first.colors.elementAt(i));
			} else {
				offspring.colors.add(second.colors.elementAt(i));
			}
		}

		for (Ellipse e : first.ellipses) {
			if (PRNG.nextBoolean() == true) {
				offspring.ellipses.add(new Ellipse(e));
			}
		}

		for (Ellipse e : second.ellipses) {
			if (PRNG.nextBoolean() == true) {
				offspring.ellipses.add(new Ellipse(e));
			}
		}
	}

	void mutate() {
		/*
		 * Mutation probability.
		 */
		if (PRNG.nextDouble() > 0.01) {
			return;
		}

		if (colorsEvolution == true) {
			offspring.colors.setElementAt(new Color(PRNG.nextInt(0x1000000)),
					PRNG.nextInt(offspring.colors.size()));
		}

		double factor = PRNG.nextDouble();
		Ellipse ellipse = offspring.ellipses
				.get(PRNG.nextInt(offspring.ellipses.size()));

		int dx = (int) (ellipse.WIDTH() * factor);
		int dy = (int) (ellipse.HEIGHT() * factor);
		double theta = 2 * Math.PI * PRNG.nextDouble();

		/*
		 * Mutate color in some cases by taking color of other ellipse.
		 */
		if (PRNG.nextDouble() < factor) {
			ellipse.color = offspring.ellipses
					.get(PRNG.nextInt(offspring.ellipses.size())).color;
		}

		/*
		 * Mutate positions.
		 */
		if (PRNG.nextBoolean() == true) {
			ellipse.x1 -= dx;
		} else {
			ellipse.x1 += dx;
		}
		if (PRNG.nextBoolean() == true) {
			ellipse.y1 -= dy;
		} else {
			ellipse.y1 += dy;
		}

		/*
		 * Mutate rotation.
		 */
		ellipse.setup((int) ((ellipse.x1 + ellipse.x2) / 2.0),
				(int) ((ellipse.y1 + ellipse.y2) / 2.0), theta);

		// TODO Ellipse should not be outside of the image.
		if (ellipse.x1 < 0 || ellipse.y1 < 0 || ellipse.x2 < 0 || ellipse.y2 < 0
				|| ellipse.x1 >= image.getWidth()
				|| ellipse.y1 >= image.getHeight()
				|| ellipse.x2 >= image.getWidth()
				|| ellipse.y2 >= image.getHeight()) {
			ellipse.setup((int) (image.getWidth() / 2.0),
					(int) (image.getHeight() / 2.0), theta);
		}
	}

	void evaluate() {
		/*
		 * Calculate the most used colors from the original picture.
		 */
		Map<Color, Integer> histogram = new HashMap<Color, Integer>();
		int pixels[] = image.getRGB(0, 0, image.getWidth(), image.getHeight(),
				null, 0, image.getWidth());
		for (int i = 0; i < pixels.length; i++) {
			Color color = new Color(pixels[i]);

			if (histogram.containsKey(color) == false) {
				histogram.put(color, 1);
			} else {
				histogram.put(color, histogram.get(color) + 1);
			}
		}

		/*
		 * Sort according color usage and x-y coordinates. The most used colors
		 * should be drawn first.
		 */
		for (int i = 0; i < offspring.ellipses.size(); i++) {
			for (int j = i + 1; j < offspring.ellipses.size(); j++) {
				Ellipse a = offspring.ellipses.get(i);
				Ellipse b = offspring.ellipses.get(j);

				if (histogram.get(a.color) == null
						|| histogram.get(b.color) == null) {
					continue;
				}

				if (histogram.get(a.color) < histogram.get(b.color)) {
					Collections.swap(offspring.ellipses, i, j);
				} else if (histogram.get(a.color) == histogram.get(b.color)) {
					if (a.x1 * a.x1 + a.y1 * a.y1 > b.x1 * b.x1 + b.y1 * b.y1) {
						Collections.swap(offspring.ellipses, i, j);
					}
				}
			}
		}

		/*
		 * Draw ellipses.
		 */
		experimental = new BufferedImage(image.getWidth(), image.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		Util.drawEllipses(experimental,
				new ArrayList<Ellipse>(offspring.ellipses));

		// TODO Number of ellipses and images distance can be used with some
		// coefficients.
		double size = offspring.ellipses.size();
		double distance = IMAGE_COMPARATOR.distance(image, experimental);
		double alpha = Util.alphaLevel(experimental, offspring.colors);
		offspring.fittnes = 0.1D * size + 0.6D * distance * distance * distance
				+ 0.3D * alpha * alpha;
	}

	void survive() {
		result.ellipses = offspring.ellipses;
		result.fittnes = offspring.fittnes;

		if (result.fittnes < best.fittnes) {
			best = result;

			/*
			 * Only for optimization progress report.
			 */
			(new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						synchronized (experimental) {
							ImageIO.write(experimental, "png", new File(
									"" + System.currentTimeMillis() + ".png"));

							BufferedOutputStream out = new BufferedOutputStream(
									new FileOutputStream(
											"" + System.currentTimeMillis()
													+ ".txt"));
							out.write(best.toString().getBytes());
							out.close();
						}
					} catch (IOException e) {
					}
				}
			})).start();
		}
	}
}
