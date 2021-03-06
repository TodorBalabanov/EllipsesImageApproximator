package eu.veldsoft.ellipses.image.approximator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.genetics.Chromosome;
import org.apache.commons.math3.genetics.ChromosomePair;
import org.apache.commons.math3.genetics.CrossoverPolicy;

class InstructionsCrossover implements CrossoverPolicy {
	/** A pseudo-random number generator instance. */
	private static final Random PRNG = new Random();

	@Override
	public ChromosomePair crossover(final Chromosome first,
			final Chromosome second) throws MathIllegalArgumentException {
		if (first instanceof EllipseListChromosome == false) {
			throw new IllegalArgumentException();
		}

		if (second instanceof EllipseListChromosome == false) {
			throw new IllegalArgumentException();
		}

		final List<Ellipse> parent1 = ((EllipseListChromosome) first)
				.getEllipses();
		final List<Ellipse> parent2 = ((EllipseListChromosome) second)
				.getEllipses();

		final List<Ellipse> child1 = new ArrayList<Ellipse>();
		final List<Ellipse> child2 = new ArrayList<Ellipse>();

		for (Ellipse ellipse : parent1) {
			if (PRNG.nextBoolean() == true) {
				child1.add(ellipse);
			}
		}
		for (Ellipse ellipse : parent2) {
			if (PRNG.nextBoolean() == true) {
				child1.add(ellipse);
			}
		}

		for (Ellipse ellipse : parent1) {
			if (PRNG.nextBoolean() == true) {
				child2.add(ellipse);
			}
		}
		for (Ellipse ellipse : parent2) {
			if (PRNG.nextBoolean() == true) {
				child2.add(ellipse);
			}
		}

		return new ChromosomePair(
				((EllipseListChromosome) first)
						.newFixedLengthChromosome(child1),
				((EllipseListChromosome) second)
						.newFixedLengthChromosome(child2));
	}

}
