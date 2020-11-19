import finance.FinancialTools;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.util.function.Supplier;

public class CashFlowsPane extends VBox
{
	public CashFlowsPane()
	{
		this.setSpacing(5);
		new CashFlow();
	}

	protected Runnable onPresentValueChanged = () -> {};
	public void setOnPresentValueChanged(Runnable onPresentValueChanged) { this.onPresentValueChanged = onPresentValueChanged; }

	public double getPresentValue(double interestRate)
	{
		double presentValue = 0;
		for (Node n : this.getChildren())
		{
			if (!(n instanceof CashFlow)) continue;
			CashFlow cf = (CashFlow)n;
			presentValue += cf.presentValue(interestRate);
		}
		return presentValue;
	}

	public class CashFlow extends HBox
	{
		private final CheckBox active = new CheckBox();
		private final ComboBox<CashFlowType> type = new ComboBox<>();
		private final TextField years = new TextField();
		private final TextField payments = new TextField();
		private final ComboBox<CashFlowCompoundPeriod> compoundPeriod = new ComboBox<>();
		private final TextField growth = new TextField();
		private final Button deleteCashFlow = new Button("×");

		public CashFlow()
		{
			type.getItems().addAll(CashFlowType.getAll());
			compoundPeriod.setEditable(true);
			compoundPeriod.setConverter(CashFlowCompoundPeriod.stringConverter);
			compoundPeriod.getItems().addAll(CashFlowCompoundPeriod.getAll());
			compoundPeriod.getSelectionModel().select(CashFlowCompoundPeriod.ANNUALLY);

			years.visibleProperty().bind(type.valueProperty().isNotNull());
			payments.visibleProperty().bind(type.valueProperty().isNotNull());
			growth.visibleProperty().bind(type.valueProperty().isEqualTo(CashFlowType.ANNUITY_UNIFORM).or(type.valueProperty().isEqualTo(CashFlowType.ANNUITY_EXPONENT)));
			compoundPeriod.visibleProperty().bind(type.valueProperty().isEqualTo(CashFlowType.ANNUITY).or(type.valueProperty().isEqualTo(CashFlowType.ANNUITY_UNIFORM)).or(type.valueProperty().isEqualTo(CashFlowType.ANNUITY_EXPONENT)));
			deleteCashFlow.visibleProperty().bind(type.valueProperty().isNotNull());

			years.setPromptText("Time(s)");
			payments.setPromptText("Payment(s)");
			growth.promptTextProperty().bind(Bindings.createStringBinding(() -> (type.getValue() == CashFlowType.ANNUITY_EXPONENT ? "Growth (%)" : "Growth"), type.valueProperty()));

			type.setPrefWidth(150);
			years.setPrefWidth(130);
			//payments.setPrefWidth(220);
			payments.prefWidthProperty().bind(this.widthProperty().subtract(833-200));
			compoundPeriod.setPrefWidth(140);
			growth.setPrefWidth(130);
			deleteCashFlow.setPrefWidth(25);

			active.selectedProperty().addListener(e -> CashFlowsPane.this.onPresentValueChanged.run());
			type.valueProperty().addListener(e -> CashFlowsPane.this.onPresentValueChanged.run());
			years.textProperty().addListener(e -> CashFlowsPane.this.onPresentValueChanged.run());
			payments.textProperty().addListener(e -> CashFlowsPane.this.onPresentValueChanged.run());
			compoundPeriod.valueProperty().addListener(e -> CashFlowsPane.this.onPresentValueChanged.run());
			growth.textProperty().addListener(e -> CashFlowsPane.this.onPresentValueChanged.run());
			type.valueProperty().addListener((e, oldValue, newValue) ->
			{
				if (oldValue != null) return;
				new CashFlow();
				active.setSelected(true);
			});
			deleteCashFlow.setOnAction(e ->
			{
				CashFlowsPane.this.getChildren().remove(this);
				CashFlowsPane.this.onPresentValueChanged.run();
			});

			this.setSpacing(5);
			this.getChildren().addAll(active, type, years, payments, compoundPeriod, growth, deleteCashFlow);
			this.setAlignment(Pos.CENTER_LEFT);
			this.prefWidthProperty().bind(CashFlowsPane.this.widthProperty());
			CashFlowsPane.this.getChildren().add(this);
		}

		public double presentValue(double interestRate)
		{
			if (!active.isSelected() || type.getValue() == null)
				return 0;
			double[] years = splitNum(this.years.getText());
			double[] payments = splitNum(this.payments.getText());
			if (type.getValue() == CashFlowType.ONETIME)
			{
				double presentValue = 0;
				for (double year : years)
					for (double payment : payments)
						presentValue += payment * FinancialTools.futureToPresentValue(interestRate, year);
				return presentValue;
			}
			if (type.getValue() == CashFlowType.ANNUITY || type.getValue() == CashFlowType.ANNUITY_EXPONENT || type.getValue() == CashFlowType.ANNUITY_UNIFORM)
			{
				double presentValue = 0;
				double compoundPeriod = this.compoundPeriod.getValue().getPeriod();
				double growthRate = 0;
				if (type.getValue() == CashFlowType.ANNUITY_EXPONENT)
					growthRate = Main.toDoubleSpecial(this.growth.getText()) / 100;
				double growthUniform = 0;
				if (type.getValue() == CashFlowType.ANNUITY_UNIFORM)
					growthUniform = Main.toDoubleSpecial(this.growth.getText());
				for (int i = 0; i < years.length; i += 2)
				{
					double timeSpan = (i + 1 < years.length) ? years[i + 1] - years[i] : Double.POSITIVE_INFINITY;
					for (double payment : payments)
					{
						presentValue += (payment + growthUniform * FinancialTools.uniformGrowthToAnnuity(interestRate, timeSpan)) *
								FinancialTools.annuityToPresentValue(interestRate - growthRate, compoundPeriod, timeSpan) *
								FinancialTools.futureToPresentValue(interestRate, years[i]);
					}
				}
				return presentValue;
			}
			return Double.NaN;
		}

		private double[] splitNum(String str)
		{
			String[] strArray = str.split(",");
			double[] resultArray = new double[strArray.length];
			int i = 0;
			for (String s : strArray)
				resultArray[i++] = Main.toDoubleSpecial(s);
			return resultArray;
		}
	}

	private static class CashFlowType
	{
		public static final CashFlowType ONETIME = new CashFlowType("One-time");
		public static final CashFlowType ANNUITY = new CashFlowType("Annuity");
		public static final CashFlowType ANNUITY_UNIFORM = new CashFlowType("Annuity (uniform growth)");
		public static final CashFlowType ANNUITY_EXPONENT = new CashFlowType("Annuity (geometric growth)");

		public final String name;
		private CashFlowType(String name) { this.name = name; }
		@Override public String toString() { return this.name; }

		public static CashFlowType[] getAll() { return new CashFlowType[] {ONETIME, ANNUITY, ANNUITY_UNIFORM, ANNUITY_EXPONENT}; }
	}

	private static class CashFlowCompoundPeriod
	{
		public static final CashFlowCompoundPeriod BIANNUALLY = new CashFlowCompoundPeriod("Biannually", 2);
		public static final CashFlowCompoundPeriod ANNUALLY = new CashFlowCompoundPeriod("Annually", 1);
		public static final CashFlowCompoundPeriod SEMIANNUALLY = new CashFlowCompoundPeriod("Semi-Annually", (double)1/2);
		public static final CashFlowCompoundPeriod QUARTERLY = new CashFlowCompoundPeriod("Quarterly", (double)1/4);
		public static final CashFlowCompoundPeriod MONTHLY = new CashFlowCompoundPeriod("Monthly", (double)1/12);
		public static final CashFlowCompoundPeriod SEMIMONTHLY = new CashFlowCompoundPeriod("Semi-Monthly", (double)1/24);
		public static final CashFlowCompoundPeriod WEEKLY = new CashFlowCompoundPeriod("Weekly", (double)7/365.25);
		public static final CashFlowCompoundPeriod DAILY = new CashFlowCompoundPeriod("Daily", (double)1/365.25);
		public static final CashFlowCompoundPeriod CONTINUOUSLY = new CashFlowCompoundPeriod("Continuously", 0);
		public static final StringConverter<CashFlowCompoundPeriod> stringConverter = new StringConverter<CashFlowCompoundPeriod>()
		{
			@Override public String toString(CashFlowCompoundPeriod object) { return object.name; }
			@Override public CashFlowCompoundPeriod fromString(String string)
			{
				if (getByName(string) != null)
					return getByName(string);
				else
					return new CashFlowCompoundPeriod(string, () -> Main.toDoubleSpecial(string));
			}
		};

		public final String name;
		private final double period;
		private final Supplier<Double> periodSupplier;
		public CashFlowCompoundPeriod(String name, Supplier<Double> supplier)
		{
			this.name = name;
			this.period = 0;
			this.periodSupplier = supplier;
		}
		private CashFlowCompoundPeriod(String name, double period)
		{
			this.name = name;
			this.period = period;
			this.periodSupplier = null;
		}
		public double getPeriod() { return periodSupplier != null ? periodSupplier.get() : this.period; }
		@Override public String toString() { return this.name; }

		public static CashFlowCompoundPeriod getByName(String name)
		{
			for (CashFlowCompoundPeriod cf : getAll())
				if (cf.name.equals(name))
					return cf;
			return null;
		}
		public static CashFlowCompoundPeriod[] getAll()
		{
			return new CashFlowCompoundPeriod[] {BIANNUALLY, ANNUALLY, SEMIANNUALLY, QUARTERLY, MONTHLY, SEMIMONTHLY, WEEKLY, DAILY, CONTINUOUSLY};
		}
	}
}
