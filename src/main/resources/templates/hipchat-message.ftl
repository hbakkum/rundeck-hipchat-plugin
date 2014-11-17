Execution of job
<a href="${execution.job.href}">
<#if execution.job.group?has_content>${execution.job.group}/</#if>${execution.job.name}</a>
<#if trigger == "start">
    <b>started</b>
<#elseif trigger == "failure">
    <b>failed</b>
<#elseif trigger == "success">
    <b>succeeded</b>
</#if>
<ul>
    <li>User: ${execution.context.job.username}</li>
    <li>ExecId: ${execution.context.job.execid}</li>
</ul>
<a href="${execution.href}">View Output</a>