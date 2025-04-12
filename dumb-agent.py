from google import genai
import json
import websocket
import random
# {
#   "type": "state",
#   "companies": { //map id -> company
#      {
#       "id": "$TSLA",
#       "name": "Tesla",
#       "description": "Description here...",
#       "performance": {
#         "currentValue": 69.0,
#         "lastValues": [60.0, 65.0, 64.0, 59.0, 50.0, 45.0]
#       }
#     },
#     ...
#   },
#   "posts": [
#     {
#       "poster": "$TSLA", // Ali pa null, če anonymous
#       "message": "Wow, $TSLA is so good, crazy!",
#     },
#     ...
#   ]
# }
def smartAgent(serverResponse):
	data=json.loads(serverResponse)
	companies=data["companies"]
	posts=data["posts"]
	prompt="You are a master stock trading agent. You must rank the companies based on their previous performance, description and social media posts. Using a complex algorith you must determine the following attributes of each company stock: Stability, Hype, Woke, Based, Cringe, Corporate newspeak and Despair. Rate each of those attributes from 0 to 1, with zero being the lowest, and one being the highest possible. Your response should be in the following json format: 'companyID:[list of all attributes in the format of attribute name: score]' for each of the attributes. Nothing else. This is the data from companies: \n" + json.dumps(companies) + "And these are the social media posts: \n" + json.dumps(posts)

	client=genai.Client(api_key="AIzaSyBRYe_6Muj9Verk09P9TX-OEZuxnRmcxck")
	response=client.models.generate_content(model="gemini-2.0-flash-lite", contents=prompt)
	cleanJson=response.text.removeprefix("```json\n").removesuffix("```")
	allAgentResponses=[]
	allAgentResponses.append(doomerAgent(cleanJson))
	allAgentResponses.append(corpoAgent(cleanJson))
	allAgentResponses.append(zoomerAgent(cleanJson))
	allAgentResponses.append(standardAgent(cleanJson))
	allAgentResponses.append(wallStreetBetsAgent(companies))

	decisions={"decisions":allAgentResponses}
	return json.dumps(decisions)


# {
#   "type": "decisions",
#   "decisions": [ //seznam mapov odgovorv agentov
#     {
#       "$TSLA": "HOLD",
#       "$GOOGLE": "BUY",
#       ...
#     },
#     ...
#   ]
# }
def doomerAgent(companyList):
	companyDict=json.loads(companyList)
	responseList={}
	for company in companyDict:
		if companyDict[company]["Despair"]>0.7:
			responseList[company]="BUY"
		elif companyDict[company]["Despair"]>0.5:
			responseList[company]="HOLD"
		else:
			responseList[company]="SELL"
	return responseList

def corpoAgent(companyList):
	companyDict=json.loads(companyList)
	responseList={}
	for company in companyDict:
		if companyDict[company]["Corporate newspeak"]>0.7:
			responseList[company]="BUY"
		elif companyDict[company]["Corporate newspeak"]>0.5:
			responseList[company]="HOLD"
		else:
			responseList[company]="SELL"
	return responseList

def wallStreetBetsAgent(companyList):
	#companyDict=json.loads(companyList)
	#newList=sorted(companyList, key=lambda i:i)
	responseList={}
	for company in companyList:
		value=company["performance"]["currentValue"]
		name=company["id"]
		if value <50:
			responseList[name]=random.choice(["BUY", "HOLD"])
		else:
			responseList[name]="SELL"
	return responseList

def zoomerAgent(companyList):
	companyDict=json.loads(companyList)
	responseList={}
	for company in companyDict:
		if companyDict[company]["Cringe"]>0.5 or companyDict[company]["Based"]>0.5:
			responseList[company]="BUY"
		elif companyDict[company]["Cringe"]>0.3 or companyDict[company]["Based"]>0.3:
			responseList[company]="HOLD"
		else:
			responseList[company]="SELL"
	return responseList

def standardAgent(companyList):
	companyDict=json.loads(companyList)
	responseList={}
	for company in companyDict:
		if companyDict[company]["Stability"]>0.5 or companyDict[company]["Hype"]>0.5:
			responseList[company]="BUY"
		elif companyDict[company]["Stability"]>0.3 or companyDict[company]["Hype"]>0.3:
			responseList[company]="HOLD"
		else:
			responseList[company]="SELL"
	return responseList


def on_message(ws, message):

    agentDecisions=smartAgent(message)
    ws.send(agentDecisions)
	#print(message)
	#ws.send(message)

def on_error(ws, error):
    print(f"Encountered error: {error}")

def on_close(ws, close_status_code, close_msg):
    print("Connection closed")

def on_open(ws):

    print("Connection opened")
    ws.send("Hello server")

if __name__ == "__main__":
	# testJson={
  # "type": "state",
  # "companies": [
  #   {
  #     "id": "$TSLA",
  #     "name": "Tesla",
  #     "description": "Description here...",
  #     "performance": {
  #       "currentValue": 69.0,
  #       "lastValues": [60.0, 65.0, 64.0, 59.0, 50.0, 45.0]
  #     }
  #   }, {"id":"$TSLA2", "name":"Tesla2", "description":"Description here2...", "performance":{"currentValue":6.0, "lastValues":[40.0, 65.0, 67.0, 49.0, 50.0, 45.0]}},
  # ],
  # "posts": [
  #   {
  #     "message": "Wow, $TSLA is so good, crazy!",
  #   },
	#   {"message":"Wow, $TSLA2 is shite", }
  # 	]
	# }
  #
	# response=smartAgent(json.dumps(testJson))
	# print(json.dumps(response))
    ws = websocket.WebSocketApp("ws://10.32.254.39:8219/agent",
                                on_message=on_message,
                                on_error=on_error,
                                on_close=on_close)
    ws.on_open = on_open
    ws.run_forever()