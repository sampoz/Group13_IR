import sys
import pylab as pl
import numpy as np
from sklearn.metrics import auc

# Globals

fig = pl.figure()
fig.canvas.set_window_title(sys.argv[1])
subplot = 411
average_interp_curve_data = []
average_interp_curve_data_stemmed = []


# Functions

def find_interp_value(recall_value, recall, precision):
    recall_zipped = zip(recall, range(len(recall)))
    more = filter(lambda x: x[0] > recall_value, recall_zipped)
    return precision[-1] if not more else max(precision[more[0][1]:])


def plot_pr_curve(interp_recall, interp_precision, recall = None, precision = None):
    global subplot

    ax = fig.add_subplot(subplot)

    # Non-interpolated curves
    if recall and precision:
        ax.plot(recall[0], precision[0], color='b', label='Precision-Recall curve non-stemmed')
        ax.plot(recall[1], precision[1], color='r', label='Precision-Recall curve stemmed')

    # Interpolated curves
    ax.plot(interp_recall, interp_precision[0], 'k', marker='o', color='g', label='11-step Interpolated Precision-Recall curve non-stemmed')
    ax.plot(interp_recall, interp_precision[1], 'k', ls='-.', linewidth=2.0, marker='s', color='black', label='11-step Interpolated Precision-Recall curve stemmed')
    pl.xlabel('Recall')
    pl.ylabel('Precision')
    pl.ylim([0.0, 1.05])
    pl.xlim([0.0, 1.05])
    
    area = auc(recall[0], precision[0]) if (recall and precision) else auc(interp_recall, interp_precision[0])
    area_stemmed = auc(recall[1], precision[1]) if (recall and precision) else auc(interp_recall, interp_precision[1])

    pl.title("Average precision (non stemmed | stemmed) = %f | %f" % (area, area_stemmed))

    handles, labels = ax.get_legend_handles_labels()
    lgd = ax.legend(handles, labels, loc='upper center', bbox_to_anchor=(0.23,-0.1))
    ax.grid('on')

    subplot += 1


# Main program

if len(sys.argv) < 3:
    print("Invalid number of arguments, you must provide the figure name and precision-recall data file for the script.")
    sys.exit()

for i in range(2, len(sys.argv), 2):
    fname = sys.argv[i]
    fname_stemmed = sys.argv[i+1]

    precision = []
    precision_stemmed = []
    recall = []
    recall_stemmed = []

    # import some data to play with
    with open(fname) as info:
        for line in info:
            data = line.split()
            precision.append(float(data[1]))
            recall.append(float(data[2]))

    with open(fname_stemmed) as info:
        for line in info:
            data = line.split()
            precision_stemmed.append(float(data[1]))
            recall_stemmed.append(float(data[2]))

    # 11 step interpolated Precision-Recall curve
    interp_precision = []
    interp_precision_stemmed = []
    interp_recall = [0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0]

    for recall_level in interp_recall:
        interp_precision.append(find_interp_value(recall_level, recall, precision))
        interp_precision_stemmed.append(find_interp_value(recall_level, recall_stemmed, precision_stemmed))

    # Add the interp_precision data to the global array
    average_interp_curve_data.append(interp_precision)
    average_interp_curve_data_stemmed.append(interp_precision_stemmed)

    # Add the subplot to the figure
    plot_pr_curve(interp_recall, [interp_precision, interp_precision_stemmed], [recall, recall_stemmed], [precision, precision_stemmed])

# Calculate the average 11-step interpolated precision-recall curve
zipped_data = zip(average_interp_curve_data[0], average_interp_curve_data[1], average_interp_curve_data[2])
summed_interp_curve = [sum(item) for item in zipped_data]
summed_interp_curve = [item/3 for item in summed_interp_curve]

zipped_data = zip(average_interp_curve_data_stemmed[0], average_interp_curve_data_stemmed[1], average_interp_curve_data_stemmed[2])
summed_interp_curve_stemmed = [sum(item) for item in zipped_data]
summed_interp_curve_stemmed = [item/3 for item in summed_interp_curve_stemmed]

plot_pr_curve([0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0], [summed_interp_curve, summed_interp_curve_stemmed])

# Show the information
pl.tight_layout(pad=0.4, w_pad=0.5, h_pad=0.7)
pl.show()
