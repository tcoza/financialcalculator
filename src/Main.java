import finance.FinancialTools;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Main extends Application
{
	@FXML public CheckBox cashFlowsCheckBox;
	@FXML public ScrollPane cashFlowsScrollPane;
	@FXML public TextField interestRateTextField;
	@FXML public TextField presentTimeTextField;
	@FXML public TextField endOfLifeTextField;
	@FXML public TextField presentValueTextField;
	@FXML public TextField EAVTextField;
	@FXML public Text missingValueIndicatorLabel;
	@FXML public TextField missingValueTextField;
	@FXML public Spinner<Integer> decimalPlacesSpinner;
	@FXML public Button copyPresentValue;
	@FXML public Button copyEAV;
	@FXML public Button copyMissingValue;

	@Override
	public void start(Stage primaryStage) throws Exception
	{
		primaryStage.setScene(new Scene(FXMLLoader.load(getClass().getResource("scene.fxml"))));
		primaryStage.setTitle("Financial Calculator");
		//primaryStage.setResizable(false);
		primaryStage.show();
	}

	@FXML
	public void initialize()
	{
		cashFlowsScrollPane.setContent(cashFlows = new CashFlowsPane());
		cashFlows.prefWidthProperty().bind(cashFlowsScrollPane.widthProperty().subtract(25));
		decimalPlacesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,100,2));

		cashFlowsCheckBox.selectedProperty().addListener(e -> cashFlowsCheckBoxValueChanged());
		cashFlowsCheckBox.indeterminateProperty().addListener(e -> cashFlowsCheckBoxValueChanged());
		interestRateTextField.textProperty().addListener(e -> refreshPresentValue());
		presentTimeTextField.textProperty().addListener(e -> refreshPresentValue());
		endOfLifeTextField.textProperty().addListener(e -> refreshPresentValue());
		decimalPlacesSpinner.valueProperty().addListener(e -> refreshPresentValue());
		cashFlows.setOnPresentValueChanged(this::refreshPresentValue);

		copyPresentValue.setOnAction(e -> toClipboard(presentValueTextField.getText()));
		copyEAV.setOnAction(e -> toClipboard(EAVTextField.getText()));
		copyMissingValue.setOnAction(e -> toClipboard(missingValueTextField.getText()));

		refreshPresentValue();
	}

	private CashFlowsPane cashFlows;
	private boolean hadMissingValue = false;
	private void refreshPresentValue()
	{
		try
		{
			refreshCashFlowsCheckBox();

			double interestRate = toDoubleSpecial(interestRateTextField.getText()) / 100;
			double presentTime = toDoubleSpecial(presentTimeTextField.getText());
			double endOfLife = !endOfLifeTextField.getText().isEmpty() ? toDoubleSpecial(endOfLifeTextField.getText()) : Double.POSITIVE_INFINITY;
			double presentValue = cashFlows.getPresentValue(interestRate) * FinancialTools.presentToFutureValue(interestRate, presentTime);
			double EAValue = presentValue / FinancialTools.annuityToPresentValue(interestRate, 1, endOfLife - presentTime);
			String format = "%." + decimalPlacesSpinner.getValue() + "f";


			presentValueTextField.setText(String.format(format, presentValue));
			EAVTextField.setText(String.format(format, EAValue));

			if (solving)
			{
				presentValueTextField.selectAll();
				EAVTextField.selectAll();
			}
			else
			{
				hadMissingValue = false;
				setMissingValueControlsVisible(false);
				presentValueTextField.setEditable(false);
				EAVTextField.setEditable(false);
			}
		}
		catch (MissingValueException ex)
		{
			presentValueTextField.setEditable(true);
			EAVTextField.setEditable(true);
			presentValueTextField.selectAll();
			EAVTextField.selectAll();
			if (!hadMissingValue)
				presentValueTextField.requestFocus();
			hadMissingValue = true;
			Main.solutionInput = ex.tentative;
		}
		catch (Exception ex)
		{
			presentValueTextField.setText(ex.getMessage());
			EAVTextField.setText(ex.getMessage());
		}
	}

	public void cashFlowsCheckBoxValueChanged()
	{
		if (!cashFlowsCheckBox.isIndeterminate())
		{
			for (CashFlowsPane.CashFlow cf : cashFlows.getCashFlowControls())
				cf.active.setSelected(cashFlowsCheckBox.isSelected());
			cashFlowsCheckBox.setAllowIndeterminate(false);
		}
	}

	public void refreshCashFlowsCheckBox()
	{
		boolean allChecked = true;
		boolean allUnchecked = true;
		for (CashFlowsPane.CashFlow cf : cashFlows.getCashFlowControls())
		{
			allChecked &= cf.active.isSelected();
			allUnchecked &= !cf.active.isSelected();
		}
		if (!allChecked && !allUnchecked)
		{
			cashFlowsCheckBox.setAllowIndeterminate(true);
			cashFlowsCheckBox.setIndeterminate(true);
		}
		else if (allChecked)
		{
			cashFlowsCheckBox.setAllowIndeterminate(false);
			cashFlowsCheckBox.setIndeterminate(false);
			cashFlowsCheckBox.setSelected(true);
		}
		else if (allUnchecked)
		{
			cashFlowsCheckBox.setAllowIndeterminate(false);
			cashFlowsCheckBox.setIndeterminate(false);
			cashFlowsCheckBox.setSelected(false);
		}
	}

	@FXML
	public void solve(ActionEvent e)
	{
		TextField output = (TextField)e.getSource();
		if (!output.isEditable()) return;

		double targetValue = Double.parseDouble(output.getText());
		{
			int indexOfDecimal = output.getText().indexOf(".");
			int targetValueDecimalPlaces = indexOfDecimal == -1 ? 0 : output.getText().length() - indexOfDecimal - 1;
			if (targetValueDecimalPlaces > decimalPlacesSpinner.getValue())
				decimalPlacesSpinner.getValueFactory().setValue(targetValueDecimalPlaces);
		}
		double marginOfError = Math.pow(10, -decimalPlacesSpinner.getValue()) / 2;
		double tentativeValue = !Double.isNaN(Main.solutionInput) ? Main.solutionInput : 0;
		Main.solving = true;
		double missingValue = SolveFunction.solve(value ->
		{
			Main.solutionInput = value;
			refreshPresentValue();
			return Double.parseDouble(output.getText());
		},
		targetValue, marginOfError, tentativeValue);
		Main.solutionInput = missingValue;
		refreshPresentValue();
		Main.solving
= false;
		this.missingValueTextField.setText(String.valueOf(missingValue));
		setMissingValueControlsVisible(true);
	}

	private void setMissingValueControlsVisible(boolean visible)
	{
		missingValueIndicatorLabel.setVisible(visible);
		missingValueTextField.setVisible(visible);
		copyMissingValue.setVisible(visible);
	}

	private void toClipboard(String str)
	{
		ClipboardContent content = new ClipboardContent();
		content.putString(str);
		Clipboard.getSystemClipboard().setContent(content);
	}

	private static boolean solving = false;
	private static double solutionInput;
	public static double toDoubleSpecial(String str) throws MissingValueException
	{
		if (str == null)
			return 0;
		if (str.endsWith("?"))
			if (solving)
				return solutionInput;
			else
				if (str.length() == 1)
					throw new MissingValueException();
				else
					throw new MissingValueException(eval(str.substring(0, str.length() - 1)));
		else
			return !str.isEmpty() ? eval(str) : 0;
	}

	/** Arithmetic expression parser.
	 * Code from stackoverflow. Thank you Boann!
	 * https://stackoverflow.com/questions/3422673/how-to-evaluate-a-math-expression-given-in-string-form
	 */
	public static double eval(final String str) {
		return new Object() {
			int pos = -1, ch;

			void nextChar() {
				ch = (++pos < str.length()) ? str.charAt(pos) : -1;
			}

			boolean eat(int charToEat) {
				while (ch == ' ') nextChar();
				if (ch == charToEat) {
					nextChar();
					return true;
				}
				return false;
			}

			double parse() {
				nextChar();
				double x = parseExpression();
				if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
				return x;
			}

			// Grammar:
			// expression = term | expression `+` term | expression `-` term
			// term = factor | term `*` factor | term `/` factor
			// factor = `+` factor | `-` factor | `(` expression `)`
			//        | number | functionName factor | factor `^` factor

			double parseExpression() {
				double x = parseTerm();
				for (;;) {
					if      (eat('+')) x += parseTerm(); // addition
					else if (eat('-')) x -= parseTerm(); // subtraction
					else return x;
				}
			}

			double parseTerm() {
				double x = parseFactor();
				for (;;) {
					if      (eat('*')) x *= parseFactor(); // multiplication
					else if (eat('/')) x /= parseFactor(); // division
					else return x;
				}
			}

			double parseFactor() {
				if (eat('+')) return parseFactor(); // unary plus
				if (eat('-')) return -parseFactor(); // unary minus

				double x;
				int startPos = this.pos;
				if (eat('(')) { // parentheses
					x = parseExpression();
					eat(')');
				} else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
					while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
					x = Double.parseDouble(str.substring(startPos, this.pos));
				} else if (ch >= 'a' && ch <= 'z') { // functions
					while (ch >= 'a' && ch <= 'z') nextChar();
					String func = str.substring(startPos, this.pos);
					x = parseFactor();
					if (func.equals("sqrt")) x = Math.sqrt(x);
					else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
					else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
					else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
					else throw new RuntimeException("Unknown function: " + func);
				} else {
					throw new RuntimeException("Unexpected: " + (char)ch);
				}

				if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

				return x;
			}
		}.parse();
	}

	private static class MissingValueException extends RuntimeException
	{
		public final double tentative;
		public MissingValueException() { this(Double.NaN); }
		public MissingValueException(double tentative) { this.tentative = tentative; }
	}
}
