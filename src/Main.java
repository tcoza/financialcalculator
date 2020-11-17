import finance.FinancialTools;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Main extends Application
{
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
		primaryStage.setResizable(false);
		primaryStage.show();
	}

	@FXML
	public void initialize()
	{
		cashFlowsScrollPane.setPrefWidth(755);
		cashFlowsScrollPane.setContent(cashFlows = new CashFlowsPane());
		decimalPlacesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,100,2));

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
			double interestRate = toDoubleSpecial(interestRateTextField.getText()) / 100;
			double presentTime = toDoubleSpecial(presentTimeTextField.getText());
			double endOfLife = !endOfLifeTextField.getText().isEmpty() ? toDoubleSpecial(endOfLifeTextField.getText()) : Double.POSITIVE_INFINITY;
			double presentValue = cashFlows.getPresentValue(interestRate) * FinancialTools.presentToFutureValue(interestRate, presentTime);
			double EAValue = presentValue / FinancialTools.annuityToPresentValue(interestRate, endOfLife - presentTime, 1);
			String format = "%." + decimalPlacesSpinner.getValue() + "f";

			hadMissingValue = false;

			presentValueTextField.setText(String.format(format, presentValue));
			EAVTextField.setText(String.format(format, EAValue));

			if (solving)
			{
				presentValueTextField.selectAll();
				EAVTextField.selectAll();
			}
			else
			{
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
		Main.solving = false;
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
					throw new MissingValueException(Double.parseDouble(str.substring(0, str.length() - 1)));
		else
			return !str.isEmpty() ? Double.parseDouble(str) : 0;
	}

	private static class MissingValueException extends RuntimeException
	{
		public final double tentative;
		public MissingValueException() { this(Double.NaN); }
		public MissingValueException(double tentative) { this.tentative = tentative; }
	}
}