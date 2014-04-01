import sys
import pylab as pl
import numpy as np
from sklearn.metrics import auc

def find_interp_value(recall_value, recall, precision):
    recall_zipped = zip(recall, range(len(recall)))
    more = filter(lambda x: x[0] > recall_value, recall_zipped)
    if not more:
        return precision[-1]
    return max(precision[more[0][1]:])

if len(sys.argv) < 3:
    print("Invalid number of arguments, you must provide the figure name and precision-recall data file for the script.")
    sys.exit()

fig = pl.figure()
fig.canvas.set_window_title(sys.argv[1])
subplot = 411

average_interp_curve_data = []

for fname in sys.argv[2:]:
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
        interp_precision.append(find_interp_value(recall_level, recall, precision))

    # Add the interp_precision data to the global array
    average_interp_curve_data.append(interp_precision)

    ax = fig.add_subplot(subplot)

    ax.plot(recall, precision, color='b', label='Precision-Recall curve')
    ax.plot(interp_recall, interp_precision, 'k', marker='o', color='g', label="11-step Interpolated Precision-Recall curve")
    pl.xlabel('Recall')
    pl.ylabel('Precision')
    pl.ylim([0.0, 1.05])
    pl.xlim([0.0, 1.05])
    pl.title('Average precision = %f' % area)

    handles, labels = ax.get_legend_handles_labels()
    lgd = ax.legend(handles, labels, loc='upper center', bbox_to_anchor=(0.23,-0.1))
    ax.grid('on')

    subplot += 1

zipped_data = zip(average_interp_curve_data[0], average_interp_curve_data[1], average_interp_curve_data[2])
summed_interp_curve = [sum(item) for item in zipped_data]
summed_interp_curve = [item/3 for item in summed_interp_curve]

ax = fig.add_subplot(subplot)

ax.plot(interp_recall, summed_interp_curve, 'k', marker='o', color='g', label="11-step Interpolated Precision-Recall curve")
pl.xlabel('Recall')
pl.ylabel('Precision')
pl.ylim([0.0, 1.05])
pl.xlim([0.0, 1.05])
pl.title('Average precision = %f' % area)

handles, labels = ax.get_legend_handles_labels()
lgd = ax.legend(handles, labels, loc='upper center', bbox_to_anchor=(0.23,-0.1))
ax.grid('on')

pl.tight_layout(pad=0.4, w_pad=0.5, h_pad=0.7)
pl.show()
