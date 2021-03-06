package neuralnetwork.network;

import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.lang.Math;
import neuralnetwork.math.*;
import neuralnetwork.exceptions.*;
import processing.core.*;

//A basic multi-layered perceptron
public class NeuralNetwork {
	
	private float lr; //Learning Rate
	private float[] inputs;
	private float[][] hidden;
	private float[] outputs;
	private float[][] errors;
	
	private Matrix[] weights;
	private float[][] biases;
	private int weightCount, biasCount;
	
	public int wbCount() {
		return weightCount + biasCount;
	}
	

	
	//CONSTRUCTOR
	//WARNING
	//DO NOT CREATE A CLASS WHERE LAYERS ARE LESS THAN THREE OR layer_neurons.length != layers
	public NeuralNetwork(int layers, int[] layer_neurons, float learning_rate){
		
		//EXCEPTIONS
		if(layers != layer_neurons.length) {
			String message = "The array length must be equal to the number of layers";
			throw new NetworkStructureException(message);
		}
		else if(layers<3) {
			String message = "Number of layers must be greater than 3";
			throw new NetworkStructureException(message);
		}

		lr = learning_rate;
		//INPUT NEURONS
		inputs = new float[layer_neurons[0]];
		
		//HIDDEN NEURONS
		hidden = new float[layers-2][];
		for(int x = 0; x<hidden.length; x++) {
			hidden[x] = new float[layer_neurons[x+1]];
		}
		//OUTPUT NEURONS
		outputs = new float[layer_neurons[layer_neurons.length - 1]];
		
		
		//INITIALIZE WEIGHTS AND BIASES
		weights = new Matrix[layers-1];
		biases = new float[layers-1][];
		
		//weights and biases between input and first hidden layer
		weights[0] = new Matrix(hidden[0].length, inputs.length);
		biases[0] = new float[hidden[0].length];
		
		//weights between layers of hidden
		for(int x = 0; x<hidden.length - 1;x++) {
			weights[x+1] = new Matrix(hidden[x+1].length, hidden[x].length);
			biases[x+1] = new float[hidden[x+1].length];
		}

		//weight matrix between last hidden layer and output
		weights[weights.length-1] = new Matrix(outputs.length, hidden[hidden.length-1].length);
		biases[biases.length - 1] = new float[outputs.length];
		
		//randomize weights between input and hidden
		weights[0].Randomize(1f/layer_neurons[0]);
		//randomize weights between hidden layers to output
		for(int x = 1; x<weights.length;x++) {
			weights[x].Randomize(1f/hidden[x-1].length);
		}
		
		//Count weights and bias
		//Weight Count
		weightCount = 0;
		for(int x = 0; x<weights.length;x++){
			weightCount += weights[x].Rows()*weights[x].Cols();
		}
		//Biases Count
		biasCount = 0;
		for(int x = 0; x<biases.length;x++) {
			biasCount += biases[x].length;
		}
	}
	
	//Returns the total number of layers the network has
	public int Layers() {
		return hidden.length + 2;
	}
	
	//Print specifications of the neural network
	public String toString() {
		String specs = "Total layers: " + Layers() + "\n"+
					   "Inputs: " + inputs.length + "\n";
		for(int x = 0; x<hidden.length; x++) {
			specs += "Hidden Layer " + (x+1) + ": " + hidden[x].length + "\n";
		}
		
		specs+="Total Weights: "+weightCount+"\n";
		specs+="Total Biases: "+biasCount+"\n";
		specs+="Outputs: " +outputs.length;
		return specs;
	}
	
	//Returns the answer of the neural network in a form of array
	public float[] FeedForward(float[] i_) {
		
		//Input to first hidden
		inputs = i_;
		Matrix inputM = new Matrix(inputs,true); // put in matrix object
		Matrix passed = Matrix.Mul(weights[0], inputM); //matrix multiply
		passed.Add(new Matrix(biases[0], true)); // add biases
		hidden[0] = Activation(passed).GetColumn(0);//pass to first hidden layer
		
		//pass the answers forward the hidden layers
		for(int x = 1; x<hidden.length;x++)	{
			inputM = new Matrix(hidden[x-1], true);//put previous layer in matrix object
			passed = Matrix.Mul(weights[x], inputM);//matrix multiply
			passed.Add(new Matrix(biases[x], true));//add biases
			hidden[x] = Activation(passed).GetColumn(0);//pass to next layer
		}
		
		//Pass the last hidden layer to get output
		inputM = new Matrix(hidden[hidden.length-1], true);//put last hidden layer to matrix object
		passed = Matrix.Mul(weights[weights.length-1], inputM);//matrix mulitply
		passed.Add(new Matrix(biases[biases.length - 1], true));// add bias
		outputs = Activation(passed).GetColumn(0);//pass to output layer
			
		return outputs;
	}
	
	public void Backpropagate(float[] i_, float[] target){
		errors = new float[weights.length][];
		float[] guess = FeedForward(i_);
		//output to last hidden layer;
		errors[errors.length - 1] = new float[outputs.length];//Instantiate last layer of errors
		for(int x = 0; x<errors[errors.length-1].length; x++) {
			errors[errors.length-1][x] = 2*(guess[x] - target[x]); //put derivatives of the cost to last layer of errors
		}
		
		//CALCULATE D COST FUNCTION (OUTPUT TO HIDDEN)
		
		errors[errors.length - 2] = new float[hidden[hidden.length -1].length];//Instantiate to the size of last hidden layer
		Matrix a1 = new Matrix(outputs);//Sigmoid
		Matrix current_e = new Matrix(outputs);

		current_e.ScalarMul(-1);
		current_e.ScalarAdd(1); //1-sigmoid
		
		current_e.HadamardProduct(a1);//sigmoid * (1 - sigmoid)
		Matrix e = new Matrix(errors[errors.length -1]);
		current_e.HadamardProduct(e);//error * sigmoid * (1 - sigmoid)
		
		
		//D COST WITH RESPECT TO PREV ACTIVATION
		Matrix prev_e = Matrix.Mul(current_e, weights[weights.length -1]);//error * sigmoid * (1 - sigmoid) * weights
		errors[errors.length-2] = prev_e.GetRow(0);//put to previous layer errors
		


		//Tune weight and bias here using current_e;
		current_e.ScalarMul(lr); //error * sigmoid * (1 - sigmoid)*learningRate;
		
		//D COST WITH RESPECT TO BIAS
		Matrix b = new Matrix(biases[biases.length - 1]);
		b.Sub(current_e);
		biases[biases.length-1] = b.GetRow(0);
		
		//D COST WITH RESPECT TO WEIGHTS
		//Transpose matrix
		Matrix current_e_t = new Matrix(current_e.GetRow(0), true);
		Matrix prevActivation = new Matrix(hidden[hidden.length-1]);
		Matrix adjustments = Matrix.Mul(current_e_t, prevActivation);
		weights[weights.length -1].Sub(adjustments);
		

		for(int x = errors.length - 2; x>=1; x--) {
			errors[x-1] = new float[hidden[x].length];
			a1 = new Matrix(hidden[x]);
			current_e = new Matrix(hidden[x]);
			
			current_e.ScalarMul(-1);
			current_e.ScalarAdd(1);
			current_e.HadamardProduct(a1);
			e = new Matrix(errors[x]);
			current_e.HadamardProduct(e);
			
			
			prev_e = Matrix.Mul(current_e, weights[x]);//error * sigmoid * (1 - sigmoid) * weights
			errors[x-1] = prev_e.GetRow(0);
			
			//Tune weight and bias here using current_e;
			b = new Matrix(biases[x]);
			current_e.ScalarMul(lr);
			b.Sub(current_e);
			
			current_e_t = new Matrix(current_e.GetRow(0), true);
			prevActivation = new Matrix(hidden[x-1]);
			adjustments = Matrix.Mul(current_e_t, prevActivation);
			weights[x].Sub(adjustments);
			
		}
		
		//adjust input weights and bias here accourding to first index of errors
		a1 = new Matrix(hidden[0]);
		current_e = new Matrix(hidden[0]);
		
		current_e.ScalarMul(-1);
		current_e.ScalarAdd(1);
		current_e.HadamardProduct(a1);
		
		e = new Matrix(errors[0]);
		current_e.HadamardProduct(e);
		
		//Tune biases
		b = new Matrix(biases[0]);
		current_e.ScalarMul(lr);
		b.Sub(current_e);
		
		//Tune weights
		current_e_t = new Matrix(current_e.GetRow(0),true);
		prevActivation = new Matrix(inputs);
		adjustments= Matrix.Mul(current_e_t, prevActivation);
		weights[0].Sub(adjustments);
		
	}
	
	
	//Get the summation of an array
//	float Summation(float[] col){
//		float sum = 0;
//		for(int x = 0; x<col.length;x++) {
//			sum += col[x];
//		}
//		return sum;
//	}
	
	//Sigmoid function
	float Activation(float input){
		return (float)(1/(1 + Math.exp(-input)));
	}
	
	Matrix Activation(Matrix m) {
		Matrix n = new Matrix(m.Rows(), m.Cols());
		for(int x = 0; x<m.Rows(); x++)	{
			for(int y = 0;y<m.Cols(); y++) {
				n.SetCell(Activation(m.GetCell(x, y)), x, y);
			}
		}
		return n;
	}
	
	
	//--------------------------SAVE AND LOAD-----------------------------
	public void LoadNeuralNetworkData(PApplet project, String filePath) {
		byte[] data = project.loadBytes(filePath);
		
		if((data.length/4) != this.wbCount()) {
			String m = "File does not match network structure";
			throw new NetworkStructureException(m);
		}
		
		int index = 0;
		
		//Load Weights
		for(int l = 0;l<weights.length;l++) {
			for(int row = 0; row<weights[l].Rows(); row++) {
				for(int col = 0; col<weights[l].Cols(); col++){
					byte[] convert = new byte[] {data[index], data[index+1],data[index+2],data[index+3]};
					float f = toFloat(convert);
					weights[l].SetCell(f, row, col);
					index+=4;
				}
			}
		}
		
		//Load Biases
		for(int x = 0; x<biases.length;x++) {
			for(int y = 0; y<biases[x].length;y++) {
				byte[] convert = new byte[] {data[index], data[index+1],data[index+2],data[index+3]};
				float f = toFloat(convert);
				biases[x][y] = f;
				index+=4;
			}
		}
		PApplet.println("Data Loaded");
	}
	public void SaveNeuralNetworkData(PApplet project) {
		//Weights and biases list;
		ArrayList<Float> wb = new ArrayList<Float>();
		//int index = 0;
		
		//PApplet.println("Getting weights...");
		//weights
		for(int l = 0;l<weights.length;l++) {
			for(int row = 0; row<weights[l].Rows(); row++) {
				for(int col = 0; col<weights[l].Cols(); col++){
					wb.add(weights[l].GetCell(row, col));
				}
			}
		}
		
		//PApplet.println("Weights Finished");
		//PApplet.println("Getting biases...");
		//biases
		for(int x = 0; x<biases.length;x++) {
			for(int y = 0; y<biases[x].length;y++) {
				wb.add(biases[x][y]);
			}
		}
		//PApplet.println("Biases Finished");
		//Put all weights in the list
		float[] numbers = new float[wb.size()];
		int index = 0;
		for(Float f:wb) {
			numbers[index] = (f!=null)?f:Float.NaN;
			index++;
		}
		
		
		byte[] data = new byte[0];
		
		for(int x = 0;x<numbers.length;x++) {
			byte[] add = toByte(numbers[x]);
			byte[] stack = data;
			data = new byte[stack.length+add.length];
			System.arraycopy(stack, 0, data, 0, stack.length);
			System.arraycopy(add, 0, data, stack.length, add.length);
		}
		
		project.saveBytes("data/neuralNetworkData.bin", data);
		PApplet.println("File Saved");
	}
	
	
	private byte[] toByte(int i){
	  return new byte[]{
	    (byte)((i >> 24) & 0xff),
	    (byte)((i >> 16) & 0xff),
	    (byte)((i >> 8) & 0xff),
	    (byte)((i >> 0) & 0xff),
	  };
	}

	private byte[] toByte(float f){
	  return toByte(Float.floatToRawIntBits(f));
	}

	float toFloat(byte[] b){
	  ByteBuffer bf = ByteBuffer.wrap(b);
	  float f = bf.getFloat();
	  return f;
	}
	
}