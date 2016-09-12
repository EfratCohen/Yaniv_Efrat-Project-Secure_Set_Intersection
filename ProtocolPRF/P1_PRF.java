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
import edu.biu.scapi.interactiveMidProtocols.ot.OTOnByteArraySInput;
import edu.biu.scapi.interactiveMidProtocols.ot.semiHonest.OTSemiHonestDDHOnByteArraySender;
import edu.biu.scapi.primitives.dlog.DlogGroup;
import edu.biu.scapi.primitives.dlog.GroupElement;
import edu.biu.scapi.primitives.dlog.miracl.MiraclDlogECFp;

public class P1_PRF
{

	/**
	 * @param args
	 */
	public static int[] stamps = new int[] {10,100,200,300,400,500,600,700,800,900,1000};
	public static int n = 1000;
	public static int commcomp = 0;
	public static void main(String[] args)
	{  // TODO Auto-generated method stub
		
		//set communication
		Channel channel = setCommunication();
		System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
		System.out.println("communication Established"); 
			
		for(int newn = 0;newn<11;newn++)
		{
			n = stamps[newn];
			System.out.println("n = "+n);
			filefill(n,'X');
			int[] SetX = new int[n];
			
			//get Input from file
			//*
			File fin;
			fin = new File("/home/yaniv/workspace/set_X"+n+".txt");
			SetX = readFile(fin);
			
			long startTime = System.currentTimeMillis();
			//test reading the file 
			//System.out.println("Set X: \n"+Arrays.toString(SetX)+"\n");
			
			// Create an underlying DlogGroup.
			DlogGroup dlog = null;
			GroupElement g = null;
			try {
				dlog = new MiraclDlogECFp();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			g = dlog.getGenerator();
			
			
			//create our algebraic PRF 
			int m = (int) Math.ceil(Math.log(10*n+1)/Math.log(2)); //bit length of all inputs.
			PRF prf = null;
			try {
				prf = new PRF(m);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			prf.generateKey(); //generate secret key;
			
			//compute Fk(x) for all x in SetX
			GroupElement[] Fk_X = new GroupElement[n];
			for(int i=0;i<n;i++)
				Fk_X[i] = prf.computeBlock(SetX[i]);
			
			//*
			for(int i=0;i<n;i++)
			{
				try {
					channel.send(Fk_X[i].generateSendableData());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				commcomp++;
			} //*/
			for(int i=0;i<n;i++)
				OblvPRF(prf, channel);
			long endTime   = System.currentTimeMillis();
			long totalTime = endTime - startTime;
			System.out.println("total runtime of P1: " + totalTime);
			System.out.println("total messages sent: " +commcomp);
			commcomp = 0;
		}
		channel.close();
		
	}
	private static void OblvPRF(PRF prf, Channel channel)
	{
		DlogGroup dlog = null;
		GroupElement g = null;
		try {
			dlog = new MiraclDlogECFp();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		g = dlog.getGenerator();
		
		int m = prf.getm();
		BigInteger[] randoms = new BigInteger[m];
		for(int i=0;i<m;i++)
			randoms[i] = prf.randomBigInteger(dlog.getOrder());
		
		BigInteger RandomMult = BigInteger.ONE;
		for(int i=0;i<m;i++)
			RandomMult = RandomMult.multiply(randoms[i]).mod(dlog.getOrder());
		
		BigInteger RandInv = RandomMult.modInverse(dlog.getOrder());
		RandInv = RandInv.multiply(prf.getKeyi(0));
		GroupElement RandExpo = dlog.exponentiate(g, RandInv);
		
		try {
			channel.send(RandExpo.generateSendableData());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		commcomp++;
		for(int i=0;i<m;i++)
		{
			BigInteger S0 = randoms[i];
			BigInteger S1 = randoms[i].multiply(prf.getKeyi(i+1));
			
			OT(S0,S1,channel);	
		}
	}
	private static void OT(BigInteger S0, BigInteger S1, Channel channel)
	{	
		OTSemiHonestDDHOnByteArraySender sender = new OTSemiHonestDDHOnByteArraySender();
		
		int max = Math.max(S0.bitLength(),S1.bitLength());
		max = (int) Math.ceil((double)(max+1)/8)+1;
		byte[] temp0 = S0.toByteArray();
		byte[] temp1 = S1.toByteArray();
		byte[] b0 = new byte[max];
		byte[] b1 = new byte[max];
		int offset;
		offset = max - temp0.length;
		for(int i=0;i<temp0.length;i++)
			b0[i+offset] = temp0[i];
		offset = max - temp1.length;
		for(int i=0;i<temp1.length;i++)
			b1[i+offset] = temp1[i];
		
		//Creates input for the sender
		OTOnByteArraySInput input = new OTOnByteArraySInput(b0, b1);

		//call the transfer part of the OT protocol
		try {
		    sender.transfer(channel, input);
		} catch (IOException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		} catch (ClassNotFoundException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		} catch (CheatAttemptException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
		commcomp++;
	}
	private static Channel setCommunication()
	{
		List<PartyData> listOfParties = null;

		LoadSocketParties loadParties = new LoadSocketParties(
				"/home/yaniv/workspace/Parties1.properties");

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
