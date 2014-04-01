import sys
import pylab as pl
import numpy as np
from sklearn.metrics import auc

def find_nearest(array, value):
    return min(range(len(array)), key=lambda i: abs(array[i]-value))

if len(sys.argv) < 2:
    print("Invalid number of arguments, you must provide the precision-recall data file for the script.")
    sys.exit()

fig = pl.figure()
subplot = 311

for fname in sys.argv[1:]:
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

    # 11 step interpolated Precision-Recall curve
    interp_precision = []
    interp_recall = [0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0]

    for recall_level in interp_recall:
        idx = find_nearest(recall, recall_level)
        interp_precision.append(max(precision[idx:]))

    #pl.clf()

    ax = fig.add_subplot(subplot)

    ax.plot(recall, precision, color='b', label='Precision-Recall curve')
    ax.plot(interp_recall, interp_precision, 'k', marker='o', color='g', label="11-step Interpolated Precision-Recall curve")
    pl.xlabel('Recall')
    pl.ylabel('Precision')
    pl.ylim([0.0, 1.0])
    pl.xlim([0.0, 1.0])
    pl.title('Average precision = %f' % area)
    #pl.legend(loc="lower left")
    #pl.show()

    handles, labels = ax.get_legend_handles_labels()
    lgd = ax.legend(handles, labels, loc='upper center', bbox_to_anchor=(0.23,-0.1))
    ax.grid('on')

    subplot += 1

pl.tight_layout(pad=0.4, w_pad=0.5, h_pad=0.7)
pl.show()
