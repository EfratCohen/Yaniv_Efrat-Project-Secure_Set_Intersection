package ProtocolPRF;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeoutException;


import edu.biu.scapi.comm.Channel;
import edu.biu.scapi.comm.twoPartyComm.LoadSocketParties;
import edu.biu.scapi.comm.twoPartyComm.NativeSocketCommunicationSetup;
import edu.biu.scapi.comm.twoPartyComm.PartyData;
import edu.biu.scapi.comm.twoPartyComm.TwoPartyCommunicationSetup;
import edu.biu.scapi.exceptions.CheatAttemptException;
import edu.biu.scapi.exceptions.DuplicatePartyException;
import edu.biu.scapi.interactiveMidProtocols.ot.OTOnByteArrayROutput;
import edu.biu.scapi.interactiveMidProtocols.ot.OTRBasicInput;
import edu.biu.scapi.interactiveMidProtocols.ot.semiHonest.OTSemiHonestDDHOnByteArrayReceiver;
import edu.biu.scapi.primitives.dlog.DlogGroup;
import edu.biu.scapi.primitives.dlog.GroupElement;
import edu.biu.scapi.primitives.dlog.GroupElementSendableData;
import edu.biu.scapi.primitives.dlog.miracl.MiraclDlogECFp;

public class P2_PRF
{

	/**
	 * @param args
	 */
	public static int[] stamps = new int[] {10,100,200,300,400,500,600,700,800,900,1000};
	public static int n = 1000;
	public static void main(String[] args)
	{  	
		//set communication
		Channel channel = setCommunication();
		System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
		System.out.println("communication Established"); 
		
		for(int newn = 0;newn<11;newn++)
		{
			n = stamps[newn];
			
			// TODO Auto-generated method stub
			filefill(n,'Y');
			int[] SetY = new int[n];
			
			//get Input from file
			//*
			File fin;
			fin = new File("/home/yaniv/workspace/set_Y"+n+".txt");
			SetY = readFile(fin);
			
			long startTime = System.currentTimeMillis();	
			
			//test reading the file 
			//System.out.println("Set Y: \n"+Arrays.toString(SetY)+"\n");
			
			DlogGroup dlog = null;
			try {
				dlog = new MiraclDlogECFp();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			GroupElement[] Fk_X = new GroupElement[n];
			GroupElement[] Fk_Y = new GroupElement[n];
			for(int i=0;i<n;i++)
				try {
					Fk_X[i] = (GroupElement)dlog.reconstructElement
						(true,(GroupElementSendableData)channel.receive());
				} catch (ClassNotFoundException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			for(int i=0;i<n;i++)
				Fk_Y[i] = OblvPRF(SetY[i], channel);
			
			LinkedList<Integer> Intersection = new LinkedList<Integer>();
			
			//*
			for(int i=0;i<n;i++)
			{
				for(int j=0;j<n;j++)
				{
					if(Fk_Y[i].equals(Fk_X[j]))
					{
						Intersection.add(SetY[i]);
						break;
					}
						
				}
			}//*/
			//print set-intersection
			System.out.println("The set-intersection is:\n"+Intersection);
			long endTime   = System.currentTimeMillis();
			long totalTime = endTime - startTime;
			System.out.println("total runtime of P2: " + totalTime);
		}
		channel.close();
		
	}
	private static GroupElement OblvPRF(int yi, Channel channel) {
		
		DlogGroup dlog = null;
		try {
			dlog = new MiraclDlogECFp();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int m = (int) Math.ceil(Math.log(10*n+1)/Math.log(2));
		GroupElement RandExpo = null;
		try {
			RandExpo = (GroupElement)dlog.reconstructElement
			(true,(GroupElementSendableData)channel.receive());
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BigInteger[] received = new BigInteger[m];
		for(int i=0;i<m;i++)
		{
			byte sigma = (byte) (yi%2);
			received[i] = OT(sigma, channel);
			yi = yi/2;
		}
		
		BigInteger RandomMult = BigInteger.ONE;
		for(int i=0;i<m;i++)
			RandomMult = RandomMult.multiply(received[i]).mod(dlog.getOrder());
		GroupElement RandOutput = dlog.exponentiate(RandExpo, RandomMult);
		
		return RandOutput;
	}
	private static BigInteger OT(byte sigma, Channel channel)
	{
		//Creates the OT receiver object.
		OTSemiHonestDDHOnByteArrayReceiver receiver = new OTSemiHonestDDHOnByteArrayReceiver();

		//Creates input for the receiver.
		OTRBasicInput input = new OTRBasicInput(sigma);
		OTOnByteArrayROutput output = null;
		
		try {
		    output = (OTOnByteArrayROutput) receiver.transfer(channel, input);
		} catch (CheatAttemptException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		} catch (IOException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		} catch (ClassNotFoundException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
		BigInteger out = new BigInteger(output.getXSigma());
		return out;
	}
	
private static Channel setCommunication()
	{
		List<PartyData> listOfParties = null;

		LoadSocketParties loadParties = new LoadSocketParties(
				"/home/yaniv/workspace/Parties0.properties");

		// Prepare the parties list.
		listOfParties = loadParties.getPartiesList();

		// Create the communication setup.
		TwoPartyCommunicationSetup commSetup = null;
		try {
			commSetup = new NativeSocketCommunicationSetup(
					listOfParties.get(0), listOfParties.get(1));
		} catch (DuplicatePartyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Map<String, Channel> connections = null;
		try {
			connections = commSetup.prepareForCommunication(1, 200000);
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Return the channel with the other party. There was only one channel
		// created.
		return (Channel) ((connections.values()).toArray())[0];
	}
	private static int[] readFile(File fin)
	{
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(fin);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 
		//Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		
		int i=0;
		int[] arr = new int[n];
		String line = null;
		try {
			while ((line = br.readLine()) != null) {
				arr[i]=Integer.parseInt(line);
				i++;
			}
		} catch (NumberFormatException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 
		try {
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return arr; 
	}
	static void filefill(int m, char Player)
	{
		
		try {

			File file = new File("/home/yaniv/workspace/set_"+Player+m+".txt");
			
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			Random randomGenerator = new Random();
			for(int i=0;i<m;i++)
			{
				bw.write(Integer.toString(randomGenerator.nextInt(10*n)+1));
				if(i<m-1)
					bw.write("\n");
			}
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
