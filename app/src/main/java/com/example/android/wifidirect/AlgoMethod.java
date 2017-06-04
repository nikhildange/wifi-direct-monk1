package com.example.android.wifidirect;

public class AlgoMethod {
	int[][] origVal,val,lines;
	int rows[],occupiedCols[]; 
	int noLine,max; 
	public AlgoMethod(int[][] matrix, int m) {
		max = m;
		origVal = matrix; 
		val = copyMatrix(matrix); 
		rows = new int[max];
		occupiedCols = new int[max];
        System.out.println("\nInput for Algorithm : ");
        printMat(val);
        ranker();
		findMax();					
		while(noLine < max){
			addMax();					
			findMax();				
		}
		opti();	
	}

	public void ranker(){
		double per = 0;
		int[] maxArray = new int[max];
		for(int r=0; r<max;r++){
			int maxV = 0;
			for(int c=0; c<max;c++){
				if (val[r][c] >= maxV) {
						maxV = val[r][c];
					}	
			}
			maxArray[r] = maxV;
		}

		for(int r=0; r<max;r++){
			for(int c=0; c<max;c++){
				per = (((double)val[r][c])/(double)maxArray[r]);
				per = per*(max);
				//System.out.println();
				//System.out.print(val[r][c]+"/"+maxArray[r]+" = "+per);
				val[r][c] = (int)(Math.round(per));//(int)(11-(Math.round(per)));

			}
		}
		System.out.println("\nOutput for Comparator Algorithm : ");
		printMat(val);
	}

	public void findMax(){
		noLine = 0;
		lines = new int[max][max];
		for(int r=0; r<max;r++){
			for(int c=0; c<max;c++){
				if(val[r][c] == max)
					neighbours(r, c, maxVH(r, c));
			}
		}
	}
	public int maxVH(int r, int c){
		int result = 0;
		for(int i=0; i<max;i++){
			if(val[i][c] == max)
				result++;
			if(val[r][i] == max)
				result--;
		}
		return result;
	}
	public void neighbours(int r, int c, int maxVH){
		if(lines[r][c] == 2)
			return;
		if(maxVH > 0 && lines[r][c] == 1) 
			return;
		if(maxVH <= 0 && lines[r][c] == -1)
			return;
		for(int i=0; i<max;i++){
			if(maxVH > 0)	
				lines[i][c] = lines[i][c] == -1 || lines[i][c] == 2 ? 2 : 1; 
			else	
				lines[r][i] = lines[r][i] == 1 || lines[r][i] == 2 ? 2 : -1;
		}
		noLine++;
	}
	public void addMax(){
		int minUncoveredValue = 0;
		for(int r=0; r<max;r++){
			for(int c=0; c<max;c++){
				if(lines[r][c] == 0 && val[r][c] > minUncoveredValue && val[r][c] < max)
					minUncoveredValue = val[r][c];
			}
		}
		minUncoveredValue = max - minUncoveredValue;
		
		for(int r=0; r<max;r++){
			for(int c=0; c<max;c++){
				if(lines[r][c] == 0) 
					val[r][c] += minUncoveredValue;
				
				else if(lines[r][c] == 2) 
					val[r][c] -= minUncoveredValue;
			}
		}
		printMat(val);
	} 
	public boolean opti(int r){
		if(r == rows.length)
			return true;
		
		for(int c=0; c<max;c++){
			if(val[r][c] == max && occupiedCols[c] == 0){
				rows[r] = c;
				occupiedCols[c] = 1; 
				if(opti(r+1))
					return true;
				occupiedCols[c] = 0; 
			}
		}
		return false;
	}
	public boolean opti(){
		return opti(0);
	} 
	public int[] result(){
		return rows;
	}
	public int total(){
		int t = 0;
		for(int r = 0; r < max; r++)
			t += origVal[r][rows[r]];
		return t;
	}
	public int[][] copyMatrix(int[][] matrix){
		int[][] t = new int[max][max];
		for(int r = 0; r < max; r++){
			t[r] = matrix[r].clone();
		}
		return t;
	}
	public void printMat(int[][] matrix){
		for(int r=0; r<max;r++){
			for(int c=0; c<max;c++){
				System.out.print(matrix[r][c]+"\t");
			}
			System.out.println();
		}
		System.out.println();
	}
}



/**/