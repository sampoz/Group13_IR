#! /bin/bash
data=../data/
mid=results
end=.txt
python pr_curve.py $1 $data$1\_$mid\0$end $data$1\_$mid\1$end $data$1\_$mid\2$end

