package csplugins.jActiveModulesHeadless;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.*;


import org.apache.commons.cli.*;
import csplugins.jActiveModulesHeadless.data.*;
import csplugins.jActiveModulesHeadless.networkUtils.*;

public class MainActiveModules {
	
	
	public enum ReadDelimiter {
		SPACE("space"," "),
		TAB("tab","\t"),
		SEMICOLON("semicolon",";");
		
		private String delimiterString;
		private String name;
		
	    ReadDelimiter(String name, String delimiter)
		{
			this.name = name;
			this.delimiterString = delimiter;
		}
	    
	    static public String getStringDelimiter(String name)
	    {
	    	String delimiter = " ";
	    	
	    	for (final ReadDelimiter method : ReadDelimiter.values()) {
				if (method.name.equals(name.toLowerCase()))
					return method.delimiterString;
			}
	    	
	    	return delimiter;
	    }
	}
	private static String usageNotes = "To be completed";
	
	private static int runCount = 0;
	
	static String dataFile = "";
	static String netFile = "";
	static String paramFile = "";
	static String outputDir = "";
	static String delimiter = "";
	static String outputFileName = "";
	
	public static void main(String[] args) {

		ActivePaths activePaths;
		ActivePathFinderParameters apfParams;
		Network inputNetwork = null;
		SifNetworkReader netReader = new SifNetworkReader();
		DataReader dataReader = new DataReader();
		SifWriter writer ;
		OutputStream outStream[];
		int dataSize = 0;
		String readDelimiter;
		FileWriter fw = null;
		Collection<Node> removeNodes = new ArrayList<Node>();
		long startTime = System.currentTimeMillis();
		
		try {
            parse(args);
        } catch (Exception e) {
            System.out.println("Parameter failure: " + e.getMessage());
            e.printStackTrace();
            System.out.println("");
            usage();
            return ;
        }
		
		
		apfParams = new ActivePathFinderParameters(paramFile);
		
		if(delimiter.isEmpty())
			readDelimiter = ReadDelimiter.getStringDelimiter(ReadDelimiter.SPACE.toString());
		else
		{
			readDelimiter = ReadDelimiter.getStringDelimiter(delimiter);
		}
		
		if(outputDir.isEmpty())
		{
			System.out.println("[WARNING] Output Directory not defined. Home directory will be used instead.");
			outputDir = System.getProperty("user.home")+File.separator+"jActiveModulesResults";
		}
			
		
		if(!netFile.isEmpty())
		{
			File file = new File(netFile);
			if(file.exists())
			{
				inputNetwork = netReader.readNetwork(file,readDelimiter);
				if(inputNetwork == null)
				{
					System.out.println("[ERROR] Network file does not contain a valid network");
					return;
				}
				apfParams.setNetwork(inputNetwork);
			}
		}
		else
			System.out.println("[WARNING] No network file defined. The algorithm can not proceed without a network.");

		if(inputNetwork == null)
			return;
		
		if(!dataFile.isEmpty())
		{
			
			File file = new File(dataFile);
			if(file.exists())
				dataSize = dataReader.readData(inputNetwork, file,readDelimiter);
			
			if(dataSize == 0)
				return;
		}
		else
		{
			System.out.println("[WARNING] No data file defined. The algorithm can not proceed without any input data.");
			return;
		}
			
		try{
			File emptyFile = new File (outputDir,"emptyNodes.txt");
			fw = new FileWriter(emptyFile);
			for(Row nodeRow : inputNetwork.getNodeTable().getAllRows())
			{
				if(nodeRow.getDataSize() == 0)
				{
					fw.write(nodeRow.getName());
					fw.flush();
					removeNodes.add(inputNetwork.getNode(nodeRow.getName()));
				}
					
			}
			if(fw != null)
				fw.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(!removeNodes.isEmpty())
		{
			inputNetwork.removeNodesKeepConnections(removeNodes);
			//inputNetwork.removeNodes(removeNodes);
			System.out.println("[WARNING] There are " + removeNodes.size() + " nodes removed from the network because there is no data for those networks");
		}
		
		apfParams.setNetwork(inputNetwork);
		
		apfParams.setSizeExpressionAttributes(dataSize);

		activePaths = new ActivePaths(inputNetwork, apfParams);

		activePaths.run();

		Network[] subnetworks = activePaths.createSubnetworks();
		
		File outFile;
		outFile = new File(outputDir);
		
		if(!outFile.exists())
			outFile.mkdirs();
		
		outStream = new OutputStream[subnetworks.length];
		File resultsFile = new File (outputDir,"jActiveModules_Search_Results.txt");
		File zipOutputFile;
		List<File> listFiles = new ArrayList<File>();
		listFiles.add(resultsFile);
		String nameFile =new SimpleDateFormat("'jActiveModuleResults-'yyyy-MM-dd hh-mm-ss'.zip'").format(new Date());
		
		if(outputFileName.isEmpty())
			zipOutputFile = new File(outputDir,nameFile);
		else
			zipOutputFile = new File(outputDir,outputFileName);
		
		String results = "Network" + readDelimiter + "Score\n";
		
		for(int i = 0; i< subnetworks.length;i++)
		{
			
			try{	
				String pathName;
				if(subnetworks[i].getName().isEmpty())
					pathName = "Module_" + (i + 1) + "_" + (new DecimalFormat("#.##").format(subnetworks[i].getScore())).toString() + ".sif";
				else
					pathName = subnetworks[i].getName() + ".sif";
				System.out.println("new network name: " + pathName);
				outFile = new File (outputDir,pathName);
				listFiles.add(outFile);
				outStream[i] = new FileOutputStream(outFile);
				writer = new SifWriter(outStream[i], subnetworks[i]);
				
				
				writer.writeSif();
				
				if(i==0)
				{
					fw = new FileWriter(resultsFile);
					results += (subnetworks[i].getName() + readDelimiter + subnetworks[i].getScore() + "\n");
				}
				else
				{
					results = (subnetworks[i].getName() + readDelimiter + subnetworks[i].getScore() + "\n");
				}
					
				if(fw != null)
				{
					fw.write(results);
					fw.flush();
				}
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		if(subnetworks.length > 0)
			runCount++;
			
		if(fw != null)
		{
			try {
				fw.write("\n\n" + apfParams.toString());
				fw.flush();
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			packZip(zipOutputFile,listFiles);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for(File files : listFiles)
			files.deleteOnExit();
		
		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		
		System.out.println("Running time: " +totalTime/60000 +  " minutes");
	}
	
	public static void parse(String[] args) throws ParseException {
        Options options = new Options();
        String netfile = "nf";
        String name = "netFile";
        Option infileOpt = OptionBuilder.withArgName(name)
                .withLongOpt(name)
                .hasArg()
                .withDescription("Input network file in sif format")
                .create(netfile);
        String dfile = "df";
        name = "dataFile";
        Option outfileOpt = OptionBuilder.withArgName(name)
                .withLongOpt(name)
                .hasArg()
                .withDescription("Input data file containning the pValues")
                .create(dfile);
        String pfile = "pf";
        name = "paramFile";
        Option algorithmOpt = OptionBuilder.withArgName(name)
                .withLongOpt(name)
                .hasArg()
                .withDescription("The parameters files that contains all parameters values")
                .create(pfile);
        
        String outDir = "o";
        name = "outputDirectory";
        Option outputOpt = OptionBuilder.withArgName(name)
                .withLongOpt(name)
                .hasArg()
                .withDescription("The output directory where results will be saved")
                .create(outDir);
        String mode = "dl";
        name = "delimiter";
        Option deliOpt = OptionBuilder.withArgName(name)
                .withLongOpt(name)
                .hasArg()
                .withDescription("Delimiter ((space) | (tab) | (semicolon)), default: space")
                .create(mode);
        String outputFile = "of";
        name = "outputFile";
        Option outFileOpt = OptionBuilder.withArgName(name)
                .withLongOpt(name)
                .hasArg()
                .withDescription("The name of zip output file")
                .create(mode);
        

        String help = "help";
        Option helpOpt = new Option(help, false, "Display this help and exit");

        options.addOption(infileOpt);
        options.addOption(outfileOpt);
        options.addOption(algorithmOpt);
        options.addOption(outputOpt);
        options.addOption(deliOpt);
        options.addOption(outFileOpt);
        

        options.addOption(helpOpt);

        CommandLineParser parser = new BasicParser();
        CommandLine line = parser.parse(options, args);

        if (line.hasOption(help)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("JActiveModuleHeadless", options);
            System.out.println("");
            System.out.println(usageNotes);
	    System.exit(0);
        }

        if (line.hasOption(netfile)) {
        	netFile = line.getOptionValue(netfile);
        }
        if (line.hasOption(dfile)) {
            dataFile = line.getOptionValue(dfile);
        }
        if (line.hasOption(pfile)) {
            paramFile = line.getOptionValue(pfile);
        }
        if (line.hasOption(outDir)) {
        	outputDir = line.getOptionValue(outDir);
        }
        if (line.hasOption(mode)) {
        	delimiter = line.getOptionValue(mode);
        }
        if (line.hasOption(outputFile)) {
        	outputFileName = line.getOptionValue(outputFile);
        }
        
        
    }
	
	static void usage() {
        try {
            parse(new String[]{"-help"});
        } catch (ParseException e1) {
            e1.printStackTrace();
        }
    }
	
	public static void packZip(File output, List<File> sources) throws IOException
    {
        System.out.println("Packaging to " + output.getName());
        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(output));
        zipOut.setLevel(Deflater.DEFAULT_COMPRESSION);

        for (File source : sources)
        {
                zipFile(zipOut, "", source);
        }
        zipOut.flush();
        zipOut.close();
        System.out.println("Done");
    }
	
	private static void zipFile(ZipOutputStream zos, String path, File file) throws IOException
    {
        if (!file.canRead())
        {
            System.out.println("Cannot read " + file.getCanonicalPath() + " (maybe because of permissions)");
            return;
        }

        System.out.println("Compressing " + file.getName());
        zos.putNextEntry(new ZipEntry(file.getName()));

        FileInputStream fis = new FileInputStream(file);

        byte[] buffer = new byte[4092];
        int byteCount = 0;
        while ((byteCount = fis.read(buffer)) != -1)
        {
            zos.write(buffer, 0, byteCount);
            //System.out.print('.');
            //System.out.flush();
        }
        //System.out.println();

        fis.close();
        zos.closeEntry();
    }

}

