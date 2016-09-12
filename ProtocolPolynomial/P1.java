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
import java.security.KeyException;
import java.security.KeyPair;
import java.security.spec.InvalidParameterSpecException;
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
import edu.biu.scapi.exceptions.DuplicatePartyException;
import edu.biu.scapi.exceptions.SecurityLevelException;
import edu.biu.scapi.midLayer.asymmetricCrypto.encryption.ElGamalEnc;
import edu.biu.scapi.midLayer.asymmetricCrypto.encryption.ScElGamalOnGroupElement;
import edu.biu.scapi.midLayer.asymmetricCrypto.keys.KeySendableData;
import edu.biu.scapi.midLayer.asymmetricCrypto.keys.ScElGamalPublicKey;
import edu.biu.scapi.midLayer.ciphertext.AsymmetricCiphertext;
import edu.biu.scapi.midLayer.ciphertext.AsymmetricCiphertextSendableData;
import edu.biu.scapi.midLayer.ciphertext.ElGamalOnGroupElementCiphertext;
import edu.biu.scapi.midLayer.plaintext.GroupElementPlaintext;
import edu.biu.scapi.primitives.dlog.DlogGroup;
import edu.biu.scapi.primitives.dlog.GroupElement;
import edu.biu.scapi.primitives.dlog.GroupElementSendableData;
import edu.biu.scapi.primitives.dlog.miracl.MiraclDlogECFp;

public class P1 
{
	public static int[] stamps = new int[] {10,100,200,300,400,500,600,700,800,900,1000};
	public static int n = 100;
	public static int commcomp = 0;
	public static void main(String[] args) throws InvalidParameterSpecException, SecurityLevelException, IOException
	{
		
		
		//set communication
		Channel channel = setCommunication();
		System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
		System.out.println("communication Established"); 
		
		for(int newn = 0;newn<11;newn++)
		{
			n = stamps[newn];
			
			filefill(n,'X');
			int[] SetX = new int[n];
			
			//get Input from file
			File fin;
			try 
			{
				fin = new File("/home/yaniv/workspace/set_X"+n+".txt");
				SetX = readFile(fin);
			}
		    catch (IOException e2)
		    {
		    	e2.printStackTrace();
			}
			GroupElementSendableData sync = null;
			channel.send(sync);
			System.out.println("n = "+n);
			long startTime = System.currentTimeMillis();	
			
			//test reading the file 
			//System.out.println("Set X: \n"+Arrays.toString(SetX)+"\n");
			
			//taking the set X and hashing it to make the polynomials.
			HashFunc hashing = new HashFunc(n);
			hashing.balancedHash(SetX);
			
			//build the polynomials from the bins.
			BigInteger[][] polys = Polynomials(hashing);

			// Create an underlying DlogGroup.
			DlogGroup dlog = null;
			GroupElement g = null;
			ElGamalEnc elGamal = null;
			try {
				dlog = new MiraclDlogECFp();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			g = dlog.getGenerator();
			//System.out.println("Generator:"+ g);
			// Create an ElGamalOnGroupElement encryption object.
			elGamal = new ScElGamalOnGroupElement(dlog);
	
			// Generate a keyPair using the ElGamal object.
			KeyPair pair = elGamal.generateKey();
			// Set private key and party2's public key:
			try {
				elGamal.setKey(pair.getPublic(), pair.getPrivate());
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
			GroupElementPlaintext currentData;
			//maybe should be Plaintext
			GroupElementPlaintext decryptedData1;
			GroupElementPlaintext decryptedData2;
			// Send the Public Key
			KeySendableData h1 = ((ScElGamalPublicKey) (elGamal.getPublicKey()))
					.generateSendableData();
			try {
				channel.send(h1);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			commcomp++;
			GroupElementPlaintext check[] = new GroupElementPlaintext[n];
			for(int j=0;j<n;j++)
				check[j] = new GroupElementPlaintext(dlog.exponentiate(g,BigInteger.valueOf(SetX[j])));
			
			// Run over the Array and send the encrypted data
			
			for (int bin=0; bin < polys.length; bin++)
			{
				for (int coef=0; coef < polys[bin].length; coef++)
				{
					// Create a GroupElementPlaintext to encrypt the plaintext.
					currentData = new GroupElementPlaintext(dlog.exponentiate(g,polys[bin][coef]));
					ElGamalOnGroupElementCiphertext cipher = (ElGamalOnGroupElementCiphertext) elGamal
							.encrypt(currentData);
					try {
						channel.send(cipher.generateSendableData());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					commcomp++;
					if(coef<polys[bin].length -1) //need to send a flag when finishing a bin and when finishing overall.
					{
						GroupElement flaggenerator = 
								dlog.exponentiate(dlog.getGenerator(), BigInteger.TEN);
						try {
							channel.send(flaggenerator.generateSendableData());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				if(bin<polys.length -1)
				{
					GroupElement flagbin = dlog.getGenerator();
					try {
						channel.send(flagbin.generateSendableData());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			GroupElement flagfinish = dlog.exponentiate(dlog.getGenerator(),BigInteger.valueOf(2));
			//send identity as a flag that all polynomials had been sent.
			
			 try {
				channel.send(flagfinish.generateSendableData());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
			// Get the encrypted data,and decrypt it
			LinkedList<Integer> Intersection = new LinkedList<Integer>();	
			ElGamalOnGroupElementCiphertext afterPolynomManipolation1;
			ElGamalOnGroupElementCiphertext afterPolynomManipolation2;
			for (int i=0; i<n;i++)
			{
				try {
					//1st bin option
					afterPolynomManipolation1 = (ElGamalOnGroupElementCiphertext) elGamal
							.reconstructCiphertext((AsymmetricCiphertextSendableData) channel
									.receive());
					commcomp++;
					decryptedData1 = (GroupElementPlaintext)elGamal
							.decrypt((AsymmetricCiphertext) afterPolynomManipolation1);
						
					//2nd bin option
					afterPolynomManipolation2 = (ElGamalOnGroupElementCiphertext) elGamal
							.reconstructCiphertext((AsymmetricCiphertextSendableData) channel
									.receive());
					commcomp++;
					decryptedData2 = (GroupElementPlaintext)elGamal
							.decrypt((AsymmetricCiphertext) afterPolynomManipolation2);
					
					for(int j=0;j<n;j++)
					{
						if(decryptedData1.equals(check[j]))
						{
							Intersection.add(SetX[j]);
							break;
						}
						else if(decryptedData2.equals(check[j]))
						{
							Intersection.add(SetX[j]);
							break;
						}
					}
		
				} catch (ClassNotFoundException | IOException | KeyException e) {
					System.out.println(e.getMessage());
				}
			}
			//print set-intersection
			//System.out.println("The set-intersection is:\n"+Intersection);
			long endTime   = System.currentTimeMillis();
			long totalTime = endTime - startTime;
			System.out.println("total runtime of P1: " + totalTime);
			System.out.println("total messages sent: "+ commcomp);
			commcomp = 0;
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
	
	private static int[] readFile(File fin) throws IOException
	{
		FileInputStream fis = new FileInputStream(fin);
	 
		//Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		
		int i=0;
		int[] arr = new int[n];
		String line = null;
		while ((line = br.readLine()) != null) {
			arr[i]=Integer.parseInt(line);
			i++;
		}
	 
		br.close();
		return arr; 
	}
	
	public static BigInteger[][] Polynomials(HashFunc hash)
	{
		int maxsize = 0;
		int bins = hash.getBinNum();
		int i;
		for(i=0;i<bins;i++)
		{
			if(maxsize<hash.getList(i).size())
				maxsize = hash.getList(i).size();
		}
		BigInteger[][] polynomials = new BigInteger[bins][maxsize+1];
		for(i=0;i<bins;i++)
			polynomials[i] = coefficients(hash.getList(i), maxsize);
		return polynomials;
	}
	public static BigInteger[] coefficients(LinkedList<Integer> list, int max)
	{
		//p(x) = (x-x1)(x-x2)...(x-xn)
		//px(x) = (x-x1)(x-x2)...(x-xk)
		//pk+1(x) = (x-xk+1)pk(x) = x*pk(x) - xk+1*pk(x)
		//build polynomial from p1(x) up to pn(x) = p(x)
		
		int size = list.size();
		BigInteger[] poly = new BigInteger[max+1];
		BigInteger[] temp = new BigInteger[max+1];
		for(int i=0;i<max+1;i++)
		{
			poly[i] = BigInteger.ZERO;
			temp[i] = BigInteger.ZERO;
		}
		BigInteger xk;
		
		poly[0]=BigInteger.valueOf(1);
		for(int i=0;i<size;i++)
		{
			xk = BigInteger.valueOf(list.get(i));
			for(int j=0;j<max;j++) //multiply pk(x) by xk+1
			{
				temp[j] = poly[j].multiply(xk);
			}
			for(int j=max-1;j>=0;j--) //shift = multiply by x
			{
				poly[j+1] = poly[j];
			}
			poly[0]=BigInteger.valueOf(0);
			for(int j=0;j<max;j++) //substuct xk+1*pk(x) from x*pk(x)
			{
				poly[j] = poly[j].subtract(temp[j]);
			}
		}
		for(int i=size;i>=0;i--)
			poly[i+max-size] = poly[i];
		for(int i = 0;i<max-size;i++)
			poly[i] = BigInteger.valueOf(0);
		return poly;
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
