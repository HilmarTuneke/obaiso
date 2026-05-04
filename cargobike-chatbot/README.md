To run:

Create .env file with the following content:

```
LLM_API_KEY=... (used in custom-chatbot.py)
LLM_BASE_URL=... (used in custom-chatbot.py)
LLM_MODEL=... (used in custom-chatbot.py)
ANTHROPIC_API_KEY=sk-... (used in chatbot.py)
```

In terminal:

```bash
pip install -r requirements.txt
python chatbot.py
```

Example questions to try:

- What cargo bikes do you have?
- How many E-Cargo Pros are in stock?
- What's the status of ORD-002?
- How much does it cost to ship 34 kg to postal code 80331?
- How much does it cost to ship SKU-CB-002 to postal code 80331?
- Are there orders for bikes out of stock?
- Give me details about BLUBB-123.
- Give me all orders from BLUBB-123.
