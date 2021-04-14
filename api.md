---
title: Xenon CWL workflow execution service API v1.0.0
language_tabs:
  - javascript: JavaScript
language_clients:
  - javascript: request
toc_footers: []
includes: []
search: false
highlight_theme: darkula
headingLevel: 2

---

<!-- Generator: Widdershins v4.0.1 -->

<h1 id="xenon-cwl-workflow-execution-service-api">Xenon CWL workflow execution service API v1.0.0</h1>

> Scroll down for code samples, example requests and responses. Select a language for code samples from the tabs above or the mobile navigation menu.

Base URLs:

* <a href="/">/</a>

License: <a href="http://www.apache.org/licenses/LICENSE-2.0.html">Apache 2.0</a>

# Authentication

* API Key (ApiKeyAuth)
    - Parameter Name: **api-key**, in: header.

<h1 id="xenon-cwl-workflow-execution-service-api-default">Default</h1>

## list of jobs

<a id="opIdgetJobs"></a>

> Code samples

```javascript

const headers = {
  'Accept':'application/json',
  'api-key':'API_KEY'
};

fetch('/jobs',
{
  method: 'GET',

  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

`GET /jobs`

get a list of all jobs, running, cancelled, or otherwise.

> Example responses

> 200 Response

```json
[
  {
    "id": "afcd1554-9604-11e6-bd3f-080027e8b32a",
    "input": {
      "file1": {
        "class": "File",
        "location": "whale.txt"
      }
    },
    "log": "http://localhost:5000/jobs/afcd1554-9604-11e6-bd3f-080027e8b32a/stderr",
    "name": "myjob1",
    "output": {
      "output": {
        "checksum": "sha1$6f9bd042bff934443cc65f7ef769613222f7b136",
        "basename": "output",
        "location": "file:///tmp/afcd1554-9604-11e6-bd3f-080027e8b32a/output",
        "path": "/tmp/afcd1554-9604-11e6-bd3f-080027e8b32a/output",
        "class": "File",
        "size": 9
      }
    },
    "state": "Success",
    "workflow": "wc-tool.cwl"
  }
]
```

<h3 id="list-of-jobs-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|list of jobs|Inline|

<h3 id="list-of-jobs-responseschema">Response Schema</h3>

Status Code **200**

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|*anonymous*|[[job](#schemajob)]|false|none|none|
|» id|string(uri)|true|none|none|
|» name|string|true|none|user supplied (non unique) name for this job|
|» workflow|string(uri)|true|none|location of the workflow|
|» input|[workflow-binding](#schemaworkflow-binding)|true|none|none|
|» state|string|true|none|none|
|» output|[workflow-binding](#schemaworkflow-binding)|true|none|none|
|» log|string(uri)|true|none|none|
|» additionalInfo|object|false|none|none|

#### Enumerated Values

|Property|Value|
|---|---|
|state|Waiting|
|state|Running|
|state|Success|
|state|Cancelled|
|state|SystemError|
|state|TemporaryFailure|
|state|PermanentFailure|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
ApiKeyAuth
</aside>

## submit a new job

<a id="opIdpostJob"></a>

> Code samples

```javascript
const inputBody = '{
  "name": "myjob1",
  "workflow": "wc-tool.cwl",
  "input": {
    "file1": {
      "class": "File",
      "location": "whale.txt"
    }
  }
}';
const headers = {
  'Content-Type':'application/json',
  'Accept':'application/json',
  'api-key':'API_KEY'
};

fetch('/jobs',
{
  method: 'POST',
  body: inputBody,
  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

`POST /jobs`

Submit a new job from a workflow definition.

> Body parameter

```json
{
  "name": "myjob1",
  "workflow": "wc-tool.cwl",
  "input": {
    "file1": {
      "class": "File",
      "location": "whale.txt"
    }
  }
}
```

<h3 id="submit-a-new-job-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|body|body|[job-description](#schemajob-description)|true|Input binding for workflow.|
|» name|body|string|false|user supplied (non unique) name for this job|
|» workflow|body|string(uri)|true|location of the workflow|
|» input|body|[workflow-binding](#schemaworkflow-binding)|false|none|

> Example responses

> 201 Response

```json
{
  "id": "afcd1554-9604-11e6-bd3f-080027e8b32a",
  "input": {
    "file1": {
      "class": "File",
      "location": "whale.txt"
    }
  },
  "log": "http://localhost:5000/jobs/afcd1554-9604-11e6-bd3f-080027e8b32a/log",
  "name": "myjob1",
  "output": {},
  "state": "Running",
  "workflow": "wc-tool.cwl"
}
```

<h3 id="submit-a-new-job-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|201|[Created](https://tools.ietf.org/html/rfc7231#section-6.3.2)|OK|[job](#schemajob)|

### Response Headers

|Status|Header|Type|Format|Description|
|---|---|---|---|---|
|201|Location|string|uri|uri of the created job|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
ApiKeyAuth
</aside>

## Get a job

<a id="opIdgetJobById"></a>

> Code samples

```javascript

const headers = {
  'Accept':'application/json',
  'api-key':'API_KEY'
};

fetch('/jobs/{jobId}',
{
  method: 'GET',

  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

`GET /jobs/{jobId}`

<h3 id="get-a-job-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|jobId|path|string|true|Job ID|

> Example responses

> 200 Response

```json
{
  "id": "afcd1554-9604-11e6-bd3f-080027e8b32a",
  "input": {
    "file1": {
      "class": "File",
      "location": "whale.txt"
    }
  },
  "log": "http://localhost:5000/jobs/afcd1554-9604-11e6-bd3f-080027e8b32a/log",
  "name": "myjob1",
  "output": {
    "output": {
      "checksum": "sha1$6f9bd042bff934443cc65f7ef769613222f7b136",
      "basename": "output",
      "location": "file:///tmp/afcd1554-9604-11e6-bd3f-080027e8b32a/output",
      "path": "/tmp/afcd1554-9604-11e6-bd3f-080027e8b32a/output",
      "class": "File",
      "size": 9
    }
  },
  "state": "Success",
  "workflow": "wc-tool.cwl"
}
```

<h3 id="get-a-job-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|Status of job|[job](#schemajob)|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|Job not found|None|

<h3 id="get-a-job-responseschema">Response Schema</h3>

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
ApiKeyAuth
</aside>

## Delete a job

<a id="opIddeleteJobById"></a>

> Code samples

```javascript

const headers = {
  'api-key':'API_KEY'
};

fetch('/jobs/{jobId}',
{
  method: 'DELETE',

  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

`DELETE /jobs/{jobId}`

Delete a job, if job is in waiting or running state then job will be cancelled first.

<h3 id="delete-a-job-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|jobId|path|string|true|Job ID|

> Example responses

<h3 id="delete-a-job-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|204|[No Content](https://tools.ietf.org/html/rfc7231#section-6.3.5)|Job deleted|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|Job not found|None|

<h3 id="delete-a-job-responseschema">Response Schema</h3>

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
ApiKeyAuth
</aside>

## Cancel a job

<a id="opIdcancelJobById"></a>

> Code samples

```javascript

const headers = {
  'Accept':'*/*',
  'api-key':'API_KEY'
};

fetch('/jobs/{jobId}/cancel',
{
  method: 'POST',

  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

`POST /jobs/{jobId}/cancel`

<h3 id="cancel-a-job-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|jobId|path|string|true|Job ID|

> Example responses

> 200 Response

```json
{
  "id": "afcd1554-9604-11e6-bd3f-080027e8b32a",
  "input": {
    "file1": {
      "class": "File",
      "location": "whale.txt"
    }
  },
  "log": "http://localhost:5000/jobs/afcd1554-9604-11e6-bd3f-080027e8b32a/log",
  "name": "myjob1",
  "output": {
    "output": {
      "checksum": "sha1$6f9bd042bff934443cc65f7ef769613222f7b136",
      "basename": "output",
      "location": "file:///tmp/afcd1554-9604-11e6-bd3f-080027e8b32a/output",
      "path": "/tmp/afcd1554-9604-11e6-bd3f-080027e8b32a/output",
      "class": "File",
      "size": 9
    }
  },
  "state": "Cancelled",
  "workflow": "wc-tool.cwl"
}
```

<h3 id="cancel-a-job-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|Job has been cancelled if job was still running or waiting|[job](#schemajob)|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|Job not found|None|

<h3 id="cancel-a-job-responseschema">Response Schema</h3>

### Response Headers

|Status|Header|Type|Format|Description|
|---|---|---|---|---|
|200|Location|string|uri|uri of the cancelled job|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
ApiKeyAuth
</aside>

## Log of a job

<a id="opIdgetJobLogById"></a>

> Code samples

```javascript

const headers = {
  'Accept':'text/plain',
  'api-key':'API_KEY'
};

fetch('/jobs/{jobId}/log',
{
  method: 'GET',

  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

`GET /jobs/{jobId}/log`

<h3 id="log-of-a-job-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|jobId|path|string|true|Job ID|

> Example responses

> 200 Response

```
"string"
```

> 302 Response

```
"[job wc-tool.cwl] /tmp/afcd1554-9604-11e6-bd3f-080027e8b32a$ wc < /tmp/afcd1554-9604-11e6-bd3f-080027e8b32a/stge84d1078-e33f-41c3-8714-aafe955d1b53/whale.txt > /tmp/afcd1554-9604-11e6-bd3f-080027e8b32a/output\nFinal process status is success\n"
```

<h3 id="log-of-a-job-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|Job log|string|
|302|[Found](https://tools.ietf.org/html/rfc7231#section-6.4.3)|Job log redirect|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|Job not found|None|

<h3 id="log-of-a-job-responseschema">Response Schema</h3>

### Response Headers

|Status|Header|Type|Format|Description|
|---|---|---|---|---|
|302|Location|string|uri|uri of the log of the job|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
ApiKeyAuth
</aside>

## Get the server status

<a id="opIdgetServerStatus"></a>

> Code samples

```javascript

const headers = {
  'Accept':'application/json',
  'api-key':'API_KEY'
};

fetch('/status',
{
  method: 'GET',

  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

`GET /status`

> Example responses

> 200 Response

```json
{
  "waiting": 3,
  "running": 1,
  "successful": 10,
  "errored": 4
}
```

<h3 id="get-the-server-status-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The server status|[status](#schemastatus)|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
ApiKeyAuth
</aside>

## A list of available workflows

<a id="opIdgetWorkflows"></a>

> Code samples

```javascript

const headers = {
  'Accept':'application/json',
  'api-key':'API_KEY'
};

fetch('/workflows',
{
  method: 'GET',

  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

`GET /workflows`

> Example responses

> 200 Response

```json
[
  {
    "filename": "echo.cwl",
    "path": "cwl/echo.cwl"
  },
  {
    "filename": "my_workflow.cwl",
    "path": "science/my_workflow.cwl"
  }
]
```

<h3 id="a-list-of-available-workflows-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The list of available workflows|Inline|

<h3 id="a-list-of-available-workflows-responseschema">Response Schema</h3>

Status Code **200**

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|*anonymous*|[[workflow](#schemaworkflow)]|false|none|none|
|» filename|string|true|none|none|
|» path|string|true|none|none|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
ApiKeyAuth
</aside>

# Schemas

<h2 id="tocS_workflow-binding">workflow-binding</h2>
<!-- backwards compatibility -->
<a id="schemaworkflow-binding"></a>
<a id="schema_workflow-binding"></a>
<a id="tocSworkflow-binding"></a>
<a id="tocsworkflow-binding"></a>

```json
{}

```

### Properties

*None*

<h2 id="tocS_job-description">job-description</h2>
<!-- backwards compatibility -->
<a id="schemajob-description"></a>
<a id="schema_job-description"></a>
<a id="tocSjob-description"></a>
<a id="tocsjob-description"></a>

```json
{
  "name": "myjob1",
  "workflow": "wc-tool.cwl",
  "input": {
    "file1": {
      "class": "File",
      "location": "whale.txt"
    }
  }
}

```

### Properties

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|name|string|false|none|user supplied (non unique) name for this job|
|workflow|string(uri)|true|none|location of the workflow|
|input|[workflow-binding](#schemaworkflow-binding)|false|none|none|

<h2 id="tocS_job">job</h2>
<!-- backwards compatibility -->
<a id="schemajob"></a>
<a id="schema_job"></a>
<a id="tocSjob"></a>
<a id="tocsjob"></a>

```json
{
  "id": "afcd1554-9604-11e6-bd3f-080027e8b32a",
  "name": "myjob1",
  "workflow": "wc-tool.cwl",
  "input": {},
  "state": "Running",
  "output": {},
  "log": "http://localhost:5000/jobs/afcd1554-9604-11e6-bd3f-080027e8b32a/log",
  "additionalInfo": {}
}

```

### Properties

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|id|string(uri)|true|none|none|
|name|string|true|none|user supplied (non unique) name for this job|
|workflow|string(uri)|true|none|location of the workflow|
|input|[workflow-binding](#schemaworkflow-binding)|true|none|none|
|state|string|true|none|none|
|output|[workflow-binding](#schemaworkflow-binding)|true|none|none|
|log|string(uri)|true|none|none|
|additionalInfo|object|false|none|none|

#### Enumerated Values

|Property|Value|
|---|---|
|state|Waiting|
|state|Running|
|state|Success|
|state|Cancelled|
|state|SystemError|
|state|TemporaryFailure|
|state|PermanentFailure|

<h2 id="tocS_file">file</h2>
<!-- backwards compatibility -->
<a id="schemafile"></a>
<a id="schema_file"></a>
<a id="tocSfile"></a>
<a id="tocsfile"></a>

```json
{
  "id": "input_file",
  "type": "File"
}

```

### Properties

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|id|string|true|none|none|
|type|string|true|none|none|

<h2 id="tocS_workflow">workflow</h2>
<!-- backwards compatibility -->
<a id="schemaworkflow"></a>
<a id="schema_workflow"></a>
<a id="tocSworkflow"></a>
<a id="tocsworkflow"></a>

```json
{
  "filename": "echo.cwl",
  "path": "cwl/echo.cwl"
}

```

### Properties

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|filename|string|true|none|none|
|path|string|true|none|none|

<h2 id="tocS_status">status</h2>
<!-- backwards compatibility -->
<a id="schemastatus"></a>
<a id="schema_status"></a>
<a id="tocSstatus"></a>
<a id="tocsstatus"></a>

```json
{
  "waiting": 2,
  "running": 2,
  "successful": 2,
  "errored": 2
}

```

### Properties

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|waiting|integer|true|none|none|
|running|integer|true|none|none|
|successful|integer|true|none|none|
|errored|integer|true|none|none|

