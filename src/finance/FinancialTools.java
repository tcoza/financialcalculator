package finance;

public class FinancialTools
{
	public static double futureToPresentValue(double interestRate, double time) { return Math.pow(1 + interestRate, -time); }
	public static double presentToFutureValue(double interestRate, double time) { return Math.pow(1 + interestRate, time); }
	public static double annuityToPresentValue(double interestRate)
		{ return annuityToPresentValue(interestRate, 1); }
	public static double annuityToPresentValue(double interestRate, double compoundPeriod)
		{ return annuityToPresentValue(interestRate, compoundPeriod, Double.POSITIVE_INFINITY); }
	public static double annuityToPresentValue(double interestRate, double compoundPeriod, double time)
	{
		if (interestRate == 0)
			return time;
		if (compoundPeriod == 0)
			return (1 - futureToPresentValue(interestRate, time)) / Math.log(1 + interestRate);
		else
			return (1 - futureToPresentValue(interestRate, time)) * compoundPeriod / (Math.pow(1 + interestRate, compoundPeriod) - 1);
	}
}
