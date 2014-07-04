#!/usr/bin/python
# demo.py - CMD Args Demo By nixCraft
import sys
import pafy

if len(sys.argv) != 3:
	sys.exit("Usage: python get_videos.py video_url content_id")

video_url = sys.argv[1]
content_id = sys.argv[2]

video = pafy.new(video_url)

best = video.getbest()
filename = "videos/" + content_id + "." + best.extension
best.download(quiet=False, filepath = filename)
