import java.util.function.Function;

public class SolveFunction
{
	public static double solve(Function<Double, Double> function) { return solve(function, 0); }
	public static double solve(Function<Double, Double> function, double targetValue) { return solve(function, targetValue, 0); }
	public static double solve(Function<Double, Double> function, double targetValue, double marginOfError) { return solve(function, targetValue, marginOfError, 0); }
	public static double solve(Function<Double, Double> function, double targetValue, double marginOfError, double tentative)
	{
		double tentativeF = function.apply(tentative);
		if (equals(tentativeF, targetValue, marginOfError))
			return tentative;

		double sizeOfInterval = Double.MIN_NORMAL;
		boolean negate = false;
		while (Double.isFinite(sizeOfInterval))
		{
			double bound = tentative + (negate ? -sizeOfInterval : sizeOfInterval);
			if (bound != tentative)		// Skip unnecessary function calls
			{
				double boundF = function.apply(bound);
				if ((targetValue > tentativeF) != (targetValue > boundF))
					return solve(function, targetValue, marginOfError, Math.min(tentative, bound), Math.max(tentative, bound));
			}
			if (negate) sizeOfInterval *= 2;
			negate = !negate;
		}
		return Double.NaN;
	}
	public static double solve(Function<Double, Double> function, double targetValue, double marginOfError, double lowerBound, double upperBound)
	{
		assert lowerBound <= upperBound;

		double lowerBoundF = function.apply(lowerBound);
		double upperBoundF = function.apply(upperBound);
		if (equals(lowerBoundF, targetValue, marginOfError))
			return lowerBound;
		if (equals(upperBoundF, targetValue, marginOfError))
			return upperBound;

		assert Math.min(lowerBoundF, upperBoundF) <= targetValue && targetValue <= Math.max(lowerBoundF, upperBoundF);

		while (Double.isFinite(lowerBound) &&
			   Double.isFinite(upperBound) &&
			   lowerBound < upperBound)
		{
			double middle = (lowerBound + upperBound) / 2;
			double middleF = function.apply(middle);

			if (equals(middleF, targetValue, marginOfError))
				return middle;
			if (middle == lowerBound || middle == upperBound)
				break;

			if (Math.min(lowerBoundF, middleF) < targetValue && targetValue < Math.max(lowerBoundF, middleF))
			{
				upperBound = middle;
				upperBoundF = middleF;
			}
			else
			{
				lowerBound = middle;
				lowerBoundF = middleF;
			}
		}
		return Double.NaN;
	}

	private static boolean equals(double v1, double v2, double moe) { return Math.abs(v1 - v2) <= moe; }
}
