<p>Execution of job #${executionData.context.job.execid}
<a href="${executionData.job.href}"><#if executionData.job.group?has_content>${executionData.job.group}/</#if>${executionData.job.name}</a>
<#if trigger == "start">
<b>started</b> by ${executionData.context.job.username}
<#elseif trigger == "failure">
<b>failed</b>
<#elseif trigger == "success">
<b>succeeded</b>
</#if>
(<a href="${executionData.href}">Open</a>)
</p>