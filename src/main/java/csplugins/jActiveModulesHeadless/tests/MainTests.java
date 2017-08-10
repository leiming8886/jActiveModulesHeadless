// ActivePaths.java:  a plugin for CytoscapeWindow,
// which uses VERA & SAM expression data
// to propose active gene regulatory paths
//------------------------------------------------------------------------------
// $Revision: 11526 $
// $Date: 2007-09-05 14:14:24 -0700 (Wed, 05 Sep 2007) $
// $Author: rmkelley $
//------------------------------------------------------------------------------
package csplugins.jActiveModulesHeadless.tests;
//------------------------------------------------------------------------------

import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;



import csplugins.jActiveModulesHeadless.data.ActivePathFinderParameters;
//import csplugins.jActiveModules.util.Scaler;
import csplugins.jActiveModulesHeadless.util.ScalerFactory;
import csplugins.jActiveModulesHeadless.*;

import csplugins.jActiveModulesHeadless.networkUtils.*;

import java.util.Collection;
//import java.io.File;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;


//-----------------------------------------------------------------------------------
public class MainTests  {


	protected int attrNamesLength;
	protected Network network;
	protected ActivePathFinderParameters apfParams;
	
	
	protected Set<Set<Node>> subnetworks;
	HashMap expressionMap;
	
	Integer option;
	String outputDir;
	String readDelimiter;
			
	public MainTests(Network Network, ActivePathFinderParameters apfParams, int option, String outputDir, String readDelimiter) {
		this.apfParams = apfParams;


		if (Network == null || Network.getNodeCount() == 0) {
			throw new IllegalArgumentException("Please select a network");
		}

		this.option = option;
		this.outputDir = outputDir;
		this.readDelimiter = readDelimiter;
		attrNamesLength = apfParams.getSizeExpressionAttributes();
		
		if (attrNamesLength == 0) {
			throw new RuntimeException("No expression data selected!");
		}
		this.network = Network;
		
		
	}
	
	public void main()
	{

		if(option == 1 || option == 2)
			generateMaxScoreDistribution();
		
		if(option == 3)
			countSubnetworks();
		
		if(option == 4)
			createSubnetwork();
		if(option == 5)
			generateAllMaxScoreDistribution();
		
		if(option == 6)
			generateScoreDistribution();
	}
	
	private void generateScoreDistribution()
	{
		ScoreTests tests;
		List<Double> scores = new ArrayList<Double>() ;
		String lineSep = System.getProperty("line.separator");
		File outFile = new File (outputDir,"scoreDistribution.txt");
		tests = new ScoreTests(network,apfParams,5);
		for(int i =0;i<100000;i++)
		{
			tests.setRandomPValues();
			scores.addAll(tests.getAllScores());
		}
		
		FileOutputStream outputStream = null;
		OutputStreamWriter outWriter = null;
		try {
			outputStream = new FileOutputStream(outFile);
			outWriter = new OutputStreamWriter(outputStream, "UTF-8");
			for(int i = 0;i<scores.size();i++)
			{
				outWriter.write(scores.get(i).toString());
				outWriter.write(lineSep);
				outWriter.flush();
			}
			outWriter.close();
			outputStream.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void generateMaxScoreDistribution()
	{
		ScoreTests tests;
		int size = apfParams.getSamplingIterationsSize();
        System.out.println("Sampling iterations size: " + size);
		ArrayList<Double> scores1 = new ArrayList<Double>();
		ArrayList<Double> scores2 = new ArrayList<Double>();
		ArrayList<Double> scores3 = new ArrayList<Double>();
        ArrayList<Double> scores4 = new ArrayList<Double>();//iryna added
        ArrayList<Double> scores5 = new ArrayList<Double>();//iryna added
		String lineSep = System.getProperty("line.separator");
		if(apfParams.getStartFromTail())
			tests = new ScoreTests(network,apfParams,network.getNodeCount()-2);
		else
            tests = new ScoreTests(network,apfParams,apfParams.getSubnetworkSize());//iryna:3-->5-->apfParams.getSubnetworkSize()
		for(int i = 0 ; i < size ; i++)
		{
			tests.setRandomPValues();
			if(option == 2)
				scores1.add(tests.getBestScoreWithIndependency());
			else
				scores1.add(tests.getBestScore());
			
		}
		/*
		if(apfParams.getStartFromTail())
			tests = new ScoreTests(network,apfParams,network.getNodeCount()-3);
		else
			tests = new ScoreTests(network,apfParams,2);//iryna:4-->2
		for(int i = 0 ; i < size ; i++)
		{
			tests.setRandomPValues();
			if(option == 2)
				scores2.add(tests.getBestScoreWithIndependency());
			else
				scores2.add(tests.getBestScore());
			
		}
        
		if(apfParams.getStartFromTail())
			tests = new ScoreTests(network,apfParams,network.getNodeCount()-4);
		else
			tests = new ScoreTests(network,apfParams,3);//iryna:5-->3
		for(int i = 0 ; i < size ; i++)
		{
			tests.setRandomPValues();
			if(option == 2)
				scores3.add(tests.getBestScoreWithIndependency());
			else
				scores3.add(tests.getBestScore());
			
		}
        //added by iryna until "File outFile;"
        if(apfParams.getStartFromTail())
            tests = new ScoreTests(network,apfParams,network.getNodeCount()-2);
        else
            tests = new ScoreTests(network,apfParams,4);//iryna:4
        for(int i = 0 ; i < size ; i++)
        {
            tests.setRandomPValues();
            if(option == 2)
                scores4.add(tests.getBestScoreWithIndependency());
            else
                scores4.add(tests.getBestScore());
            
        }
        if(apfParams.getStartFromTail())
            tests = new ScoreTests(network,apfParams,network.getNodeCount()-2);
        else
            tests = new ScoreTests(network,apfParams,5);//iryna:5
        for(int i = 0 ; i < size ; i++)
        {
            tests.setRandomPValues();
            if(option == 2)
                scores5.add(tests.getBestScoreWithIndependency());
            else
                scores5.add(tests.getBestScore());
            
        }
         */
        File outFile;
		if(option == 2)
			outFile = new File (outputDir,"scoreTestInd.txt");
		else
			outFile = new File (outputDir,"scoreTest.txt");
		FileOutputStream outputStream = null;
		OutputStreamWriter outWriter = null;
		try {
			outputStream = new FileOutputStream(outFile);
		    
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			outWriter = new OutputStreamWriter(outputStream, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			for(int i = 0;i<scores1.size();i++)
			{
                outWriter.write(scores1.get(i).toString());
                outWriter.write(readDelimiter);
                /*outWriter.write(scores2.get(i).toString());
                outWriter.write(readDelimiter);
                
                outWriter.write(scores3.get(i).toString());
                outWriter.write(readDelimiter);
                //added by iryna until outWriter.flush();
                outWriter.write(scores4.get(i).toString());
                outWriter.write(readDelimiter);
                outWriter.write(scores5.get(i).toString());
                 */
                outWriter.write(lineSep);
                outWriter.flush();
               
			}
			outWriter.close();
			outputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void generateAllMaxScoreDistribution()
	{
		ScoreTests tests;
		int size = apfParams.getSamplingIterationsSize();
		ArrayList<ArrayList<Double>> scores = new ArrayList<ArrayList<Double>>();
		String lineSep = System.getProperty("line.separator");
		tests = new ScoreTests(network,apfParams);
		int netSize = network.getNodeCount(); 
		int length = 300;
		System.out.println("start sampling");
		
		for(int i = 3; i< (netSize -1);i++)
		{
			ArrayList<Double> scores1 = new ArrayList<Double>();
			tests.generateSampleSubnetworks(i, length);
			System.out.println("Generate sample distribution of length " + length + " for subnetwork of size " + i );
			for(int j = 0 ; j < size ; j++)
			{
				tests.setRandomPValues();
				scores1.add(tests.getBestScore());
			}
			System.out.println("Generate sample distribution of length " + length + " for subnetwork of size " + i + " DONE");
			if(i < (netSize/2+1))
				length += 500;
			else
				length -= 530;
			scores.add(scores1);
			
		}
		
		File outFile = new File (outputDir,"sampleAllScores.txt");
		FileOutputStream outputStream = null;
		OutputStreamWriter outWriter = null;
		try {
			outputStream = new FileOutputStream(outFile);
		    
			outWriter = new OutputStreamWriter(outputStream, "UTF-8");
			for(int j =0 ; j< size; j++)
			{
				for(int i = 0;i<scores.size();i++)
				{
				
					outWriter.write(scores.get(i).get(j).toString());
					outWriter.write(readDelimiter);
					
				}
				outWriter.write(lineSep);
				outWriter.flush();
			}
			outWriter.close();
			outputStream.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void countSubnetworks()
	{
		ScoreTests tests = new ScoreTests(network,apfParams);
		
		if(apfParams.getStartFromTail())
			tests.getSubNetworkSizesDown(1);
		else
			tests.getSubNetworkSizes(network.getNodeCount());
	}
	
	private void createSubnetwork()
	{
		ScoreTests tests = new ScoreTests(network,apfParams);
		
		tests.setRandomPValues();
		
		Network[] subnetwork = tests.generateSubnetwork(apfParams.getSubnetworkSize());
		
		File outFile = new File (outputDir,"subnetworkSize" + apfParams.getSubnetworkSize()+".sif");
		FileOutputStream outStream = null;
		SifWriter writer = null;
		try {
			outStream = new FileOutputStream(outFile);
			writer = new SifWriter(outStream, subnetwork[0]);
			writer.writeSif(readDelimiter);
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
	}

	
	
	
	
} // class ActivePaths (a CytoscapeWindow plugin)
