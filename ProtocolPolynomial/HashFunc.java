package ProtocolPolynomial;

import java.io.IOException;
import java.util.LinkedList;

public class HashFunc 
{
	public static int n;
	private int overhead;
	private int BinNum;
	private LinkedList<Integer>[] Hashtable;
	
	@SuppressWarnings("unchecked")
	public HashFunc(int num) throws IOException
	{
		n = num;
		overhead = (int)Math.ceil(Math.log(Math.log(n)/Math.log(2))/Math.log(2));
		//overhead = (int)Math.ceil(Math.log(n)/Math.log(2));
		BinNum = n/overhead +overhead;
		BinNum = nearestPrime(BinNum);
		Hashtable = new LinkedList[BinNum];
		for(int i=0;i<BinNum;i++)
			Hashtable[i] = new LinkedList<Integer>();
	}
	void simpleHash(int[] set)
	{
		int bin;
		for(int i=0; i<n; i++)
		{
			bin = (5*(set[i]^2))%BinNum;
			//bin = set[i]%BinNum;
			Hashtable[bin].add(set[i]);
		}
	}
	void balancedHash(int[] set)
	{
		int bin1, bin2;
		for(int i=0; i<n; i++)
		{
			bin1 = (set[i]*3 + 5*(set[i]^2))%BinNum;
			bin2 = (set[i]*11 + 7*(set[i]^2))%BinNum;
			if(Hashtable[bin1].size() > Hashtable[bin2].size())
				Hashtable[bin2].add(set[i]);
			else
				Hashtable[bin1].add(set[i]);
		}
	}
	void printHash()
	{
		for (int i = 0; i<BinNum;i++)
			System.out.println(Hashtable[i]);
	}
	int getBinNum()
	{
		return BinNum;
	}
	LinkedList<Integer> getList(int i)
	{
		return Hashtable[i];
	}
	int returnSimple(int i)
	{
		return (5*(i^2))%BinNum;
	}
	int[] returnBalanced(int i)
	{
		int[] both = new int[2];
		both[0] = (i*3 + 5*(i^2))%BinNum;
		both[1] = (i*11 + 7*(i^2))%BinNum;
		return both;
	}
	static private boolean isPrime(int number)
    {
        int boundary = (int)Math.ceil(Math.sqrt(number));

        if (number <= 1) return false;
        if (number == 2) return true;

        for (int i = 2; i <= boundary; ++i)
        {
            if (number % i == 0) return false;
        }

        return true;
    }
	static private int nearestPrime(int num)
	{
		int next = num;
		while(!isPrime(next))
			next++;
		return next;
	}

}
