package finance;

public class FinancialTools
{
	public static double futureToPresentValue(double interestRate, double time) { return Math.pow(1 + interestRate, -time); }
	public static double presentToFutureValue(double interestRate, double time) { return Math.pow(1 + interestRate, time); }
	public static double annuityToPresentValue(double interestRate)
		{ return annuityToPresentValue(interestRate, 1); }
	public static double annuityToPresentValue(double interestRate, double compoundPeriod)
		{ return annuityToPresentValue(interestRate, Double.POSITIVE_INFINITY, compoundPeriod); }
	public static double annuityToPresentValue(double interestRate, double time, double compoundPeriod)
	{
		if (compoundPeriod == 0)
			return (1 - futureToPresentValue(interestRate, time)) / Math.log(1 + interestRate);
		else
			return (1 - futureToPresentValue(interestRate, time)) * compoundPeriod / (Math.pow(1 + interestRate, compoundPeriod) - 1);
	}
}
