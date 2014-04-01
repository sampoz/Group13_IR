import sys
import pylab as pl
import numpy as np
from sklearn.metrics import auc

if len(sys.argv) < 2:
    print("Invalid number of arguments, you must provide the precision-recall data file for the script.")
    sys.exit()

fname = sys.argv[1]
precision = []
recall = []

# import some data to play with
with open(fname) as info:
    for line in info:
        data = line.split()
        precision.append(float(data[1]))
        recall.append(float(data[2]))

# Compute Precision-Recall and plot curve
area = auc(recall, precision)

pl.clf()
pl.plot(recall, precision, label='Precision-Recall curve')
pl.xlabel('Recall')
pl.ylabel('Precision')
pl.ylim([0.0, 1.05])
pl.xlim([0.0, 1.0])
pl.title('Precision-Recall example: AUC=%0.2f' % area)
pl.legend(loc="lower left")
pl.show()
