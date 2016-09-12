package ProtocolPolynomial;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import org.bouncycastle.util.BigIntegers;

import edu.biu.scapi.comm.Channel;
import edu.biu.scapi.comm.twoPartyComm.LoadSocketParties;
import edu.biu.scapi.comm.twoPartyComm.NativeSocketCommunicationSetup;
import edu.biu.scapi.comm.twoPartyComm.PartyData;
import edu.biu.scapi.comm.twoPartyComm.TwoPartyCommunicationSetup;
import edu.biu.scapi.exceptions.DuplicatePartyException;
import edu.biu.scapi.exceptions.SecurityLevelException;
import edu.biu.scapi.midLayer.asymmetricCrypto.encryption.ElGamalEnc;
import edu.biu.scapi.midLayer.asymmetricCrypto.encryption.ScElGamalOnGroupElement;
import edu.biu.scapi.midLayer.asymmetricCrypto.keys.KeySendableData;
import edu.biu.scapi.midLayer.ciphertext.AsymmetricCiphertextSendableData;
import edu.biu.scapi.midLayer.ciphertext.ElGamalOnGroupElementCiphertext;
import edu.biu.scapi.midLayer.plaintext.GroupElementPlaintext;
import edu.biu.scapi.primitives.dlog.DlogGroup;
import edu.biu.scapi.primitives.dlog.GroupElement;
import edu.biu.scapi.primitives.dlog.GroupElementSendableData;
import edu.biu.scapi.primitives.dlog.miracl.MiraclDlogECFp;


public class P2_Simple 
{
	public static int[] stamps = new int[] {10,100,200,300,400,500,600,700,800,900,1000};
	public static int n = 100;
	public static void main(String[] args) throws IOException, ClassNotFoundException, InvalidKeyException
	{
		
		
		//set communication
		Channel channel = setCommunication();
		System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
		System.out.println("communication Established");
		
		for(int newn = 0;newn<11;newn++)
		{
			n = stamps[newn];
			
			filefill(n,'Y');
			int[] SetY = new int[n];
			
			//get Input from file
			File fin;
			try 
			{
			fin = new File("/home/yaniv/workspace/set_Y"+n+".txt");
			SetY = readFile(fin);
			}
		    catch (IOException e2)
		    {
			e2.printStackTrace();
			}
			//test reading the file 
			//System.out.println(Arrays.toString(SetY));
			
			channel.receive();
			System.out.println("n = "+n);
			long startTime = System.currentTimeMillis();
		
			//set the group
			DlogGroup dlog = null;
			GroupElement g = null;
			ElGamalEnc elGamal = null;
			try {
				dlog = new MiraclDlogECFp();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
			// Create an ElGamalOnGroupElement encryption object.
			try {
				elGamal = new ScElGamalOnGroupElement(dlog);
			} catch (SecurityLevelException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	
			//get publickey
			try {
				KeySendableData trying = (KeySendableData) channel.receive();
				PublicKey pk = elGamal.reconstructPublicKey(trying);
				elGamal.setKey(pk);
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//setting up flags before receiving the polynomials
			int BinNum = 1, Max = 1;
			GroupElement flag;
			GroupElement flagfinish= dlog.exponentiate(dlog.getGenerator(),BigInteger.valueOf(2));
			GroupElement flagmax = dlog.getGenerator();
			ElGamalOnGroupElementCiphertext encryptedData;
			LinkedList<ElGamalOnGroupElementCiphertext> list = new LinkedList<ElGamalOnGroupElementCiphertext>();
			do{
				encryptedData = (ElGamalOnGroupElementCiphertext) elGamal
						.reconstructCiphertext((AsymmetricCiphertextSendableData) channel.receive());
				//saving all the received data in a list, because P2 doesnt know yet the number of polynomials and max degree.
				list.addFirst(encryptedData);
				//receiving flag.
				flag = (GroupElement)dlog.reconstructElement
						(true,(GroupElementSendableData)channel.receive());
				if(!flag.equals(flagmax) && BinNum == 1)
					Max++; //max degree. 
				else if(flag.equals(flagmax))
					BinNum++;	//number of bins = number of polynomials.
			}while(!flag.equals(flagfinish));
			
			ElGamalOnGroupElementCiphertext[][] polys = new ElGamalOnGroupElementCiphertext[BinNum][Max];
			for (int i = 0; i < BinNum; i++) 
				for(int j=0;j<Max;j++)
					polys[i][j] = list.removeLast(); //converts the list to the array that represents the list of polynomials.
			
			GroupElement[] c1 = new GroupElement[Max];
			GroupElement[] c2 = new GroupElement[Max];
			int Bin;
			HashFunc hash = new HashFunc(n);
			
			for (int i=0;i<n;i++) 
			{
				Bin = hash.returnSimple(SetY[i]);
				for(int j=0;j<Max;j++)
				{
					//enc(ai)^y^i		//we do it all for bin0 and bin1
					c1[j]=dlog.exponentiate(polys[Bin][j].getC1(), BigInteger.valueOf(SetY[i]).pow(j));
					c2[j]=dlog.exponentiate(polys[Bin][j].getC2(), BigInteger.valueOf(SetY[i]).pow(j));
				}
				
				
				for (int j=1;j<Max ;j++)
				{ //this gives us P(yi)
					c1[0]=dlog.multiplyGroupElements(c1[0],c1[j]);
					c2[0]=dlog.multiplyGroupElements(c2[0],c2[j]);
				}
				//multiply the polynomial by a random number.
				SecureRandom random = new SecureRandom();
				BigInteger q = dlog.getOrder();
				BigInteger qMinusOne = q.subtract(BigInteger.ONE);
				BigInteger r = BigIntegers.createRandomInRange(BigInteger.ZERO, qMinusOne, random);
				c1[0]=dlog.exponentiate(c1[0],r);
				c2[0]=dlog.exponentiate(c2[0],r);
				
				//add yi. if P(yi) = 0 we will be left with g^yi.
				GroupElementPlaintext gbyi = new GroupElementPlaintext(dlog.exponentiate(dlog.getGenerator(),BigInteger.valueOf(SetY[i])));
				ElGamalOnGroupElementCiphertext encgbyi = (ElGamalOnGroupElementCiphertext) elGamal.encrypt(gbyi);
				c1[0]=dlog.multiplyGroupElements(c1[0],encgbyi.getC1());
				c2[0]=dlog.multiplyGroupElements(c2[0],encgbyi.getC2());
				//now we have g^(r*P(y)+y)
				
				//sending the encryption to P1
				
				ElGamalOnGroupElementCiphertext encrytedDataAfterMultiply = new ElGamalOnGroupElementCiphertext(c1[0],c2[0]);
				try {
					channel.send(encrytedDataAfterMultiply.generateSendableData());
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
			}
			long endTime   = System.currentTimeMillis();
			long totalTime = endTime - startTime;
			System.out.println("total runtime of P2: " + totalTime);
		}
		channel.close();
	}
	
	/**
	 * 
	 * Loads parties from a file and sets up the channel.
	 * 
	 * @return the channel with the other party.
	 */
	private static Channel setCommunication()
	{
		List<PartyData> listOfParties = null;

		LoadSocketParties loadParties = new LoadSocketParties(
				"/home/yaniv/workspace/Parties0.properties");

		// Prepare the parties list.
		listOfParties = loadParties.getPartiesList();

		// Create the communication setup.
		TwoPartyCommunicationSetup commSetup = null;
		try 
		{
			commSetup = new NativeSocketCommunicationSetup(
					listOfParties.get(0), listOfParties.get(1));
		} catch (DuplicatePartyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Map<String, Channel> connections = null;
		try 
		{
			connections = commSetup.prepareForCommunication(1, 200000);
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Return the channel with the other party. There was only one channel
		// created.
		return (Channel) ((connections.values()).toArray())[0];
	}
	/*
	 * read the input from a file 
	 */
	private static int[] readFile(File fin) throws IOException
	{
		FileInputStream fis = new FileInputStream(fin);
	 
		//Construct BufferedReader from InputStreamReader
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			
			int i=0;
			int[] arr = new int[n];
			String line = null;
			while ((line = br.readLine()) != null) 
			{
				arr[i]=Integer.parseInt(line);
				i++;
			}
		 
			br.close();
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
