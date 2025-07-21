import { getConventionalMaintenancesByStreetId, getLedMaintenancesByStreetId } from "../db/queries";
import { getFileAsBase64, toSaoPauloTime } from "../util/util";
import path from 'path';
import fs from 'fs';

export const maintenanceReport = async (
  reportFile: string,
  maintenanceId: string,
  streetIds: string[]
): Promise<string | undefined> => {
  if (reportFile === 'conventional') {
    return conventionalReport(maintenanceId, streetIds);
  } else {
    return ledReport(maintenanceId, streetIds); // Se tiver outro tipo no futuro, trate aqui
  }
}

const conventionalReport = async (
  maintenanceId: string,
  streetIds: string[]
) => {
  try {
    const data = await getConventionalMaintenancesByStreetId(maintenanceId, streetIds);
    if (!data) {
      console.log("Nenhum dado encontrado");
      return;
    }

    const {
      company,
      contract,
      maintenance,
      team,
      streets,
      total_by_item
    } = data;

    const templatePath = path.join(__dirname, '..', 'templates/maintenance', 'maintenance_conventional.html');
    const htmlTemplate = fs.readFileSync(templatePath, 'utf-8');

    let observations = '';
    const streetsLines = streets.map((line: any, index: number) => {
      if (line.comment) {
        observations += `${line.comment.trim().replace(/\.*$/, '')}. `;
      }
      return `
      <tr>
        <td>${index + 1}</td>
        <td>${line.address}</td>
        <td>${line.relay}</td>
        <td>${line.connection}</td>
        <td>${line.bulb}</td>
        <td>${line.sodium}</td>
        <td>${line.mercury}</td>
        <td>${line.power}</td>
        <td>${line.external_reactor}</td>
        <td>${line.internal_reactor}</td>
        <td>${line.relay_base}</td>
      </tr>
    `;
    }).join('\n');

    const companyLogo = await getFileAsBase64(company.bucket, company.company_logo);
    const utcDateOfVisit: string = maintenance.date_of_visit;    
    const dateOfVisit = toSaoPauloTime(utcDateOfVisit);
    
    const hasPending = !!maintenance.pending_points;

    let htmlFilled = htmlTemplate
      .replace('{{BASE64_LOGO_IMAGE}}', companyLogo ?? '')
      .replace('{{CONTRACT_NUMBER}}', contract.contract_number ?? '')
      .replace('{{COMPANY_SOCIAL_REASON}}', company.social_reason ?? '')
      .replace('{{COMPANY_CNPJ}}', company.company_cnpj ?? '')
      .replace('{{COMPANY_ADDRESS}}', company.company_address ?? '')
      .replace('{{COMPANY_PHONE}}', company.company_phone ?? '')
      .replace('{{CONTRACTOR_SOCIAL_REASON}}', contract.contractor ?? '')
      .replace('{{CONTRACTOR_CNPJ}}', contract.cnpj ?? '')
      .replace('{{CONTRACTOR_ADDRESS}}', contract.address ?? '')
      .replace('{{CONTRACTOR_PHONE}}', contract.phone ?? '')
      .replace('{{STREET_LINES}}', streetsLines)
      .replace('{{RELAY_TOTAL}}', total_by_item.relay)
      .replace('{{CONNECTION_TOTAL}}', total_by_item.connection)
      .replace('{{BULB_TOTAL}}', total_by_item.bulb)
      .replace('{{SODIUM_TOTAL}}', total_by_item.sodium)
      .replace('{{MERCURY_TOTAL}}', total_by_item.mercury)
      .replace('{{EXTERNAL_REACTOR_TOTAL}}', total_by_item.external_reactor)
      .replace('{{INTERNAL_REACTOR_TOTAL}}', total_by_item.internal_reactor)
      .replace('{{BASE_TOTAL}}', total_by_item.relay_base)
      .replace('{{OBSERVATIONS}}', observations)
      .replace('{{PENDING}}', hasPending ? 'checked' : '')
      .replace('{{NO_PENDING}}', hasPending ? '' : 'checked')
      .replace('{{PENDING_QUANTITY}}', maintenance.quantity_pending_points)
      .replace('{{LOCAL}}', maintenance.type)
      .replace('{{DATE_OF_VISIT}}', dateOfVisit.toFormat('dd/MM/yyyy'))
      .replace('{{ELECTRICIAN_NAME}}', `${team.electrician.name} ${team.electrician.last_name}`)
      .replace('{{DRIVER_NAME}}', `${team.driver.name} ${team.driver.last_name}`)

    if (maintenance.signature_uri) {
      const signatureImage = await getFileAsBase64(company.bucket, maintenance.signature_uri);
      const utcSignDate: string = maintenance.sign_date;
      const signDate = toSaoPauloTime(utcSignDate);
      
      const signSection =
        `
          <table >
              <thead>
                  <tr>
                      <th colspan="2" class="cell-title" style="text-align: center;">
                          ASSINATURA DO RESPONSÁVEL PELO ACOMPANHAMENTO DO SERVIÇO
                      </th>
                  </tr>
              </thead>
              <tbody>
                  <tr>
                      <td colspan="2" style="text-align: center; padding: 10px 0;">
                          <img src="data:image/png;base64,${signatureImage}" alt="Assinatura"
                              style="max-width: 250px; height: auto;">
                      </td>
                  </tr>
                  <tr>
                      <td colspan="2" style="text-align: center; padding: 4px;">
                          <p style="margin: 0; font-size: 10px; color: #555;">
                              Assinado digitalmente em: <strong>${signDate.toFormat("dd/MM/yyyy 'às' HH:mm")}</strong>
                          </p>
                      </td>
                  </tr>
                  <tr>
                      <td colspan="2">
                          <p class="label">Responsável:</p>
                          <p class="cell-text">${maintenance.responsible ?? ''}</p>
                      </td>
                  </tr>
              </tbody>
          </table>        
        `;

      htmlFilled = htmlFilled
        .replace('{{SIGN_SECTION}}', signSection)
    } else {
      htmlFilled = htmlFilled
        .replace('{{SIGN_SECTION}}', '')
    }

    return htmlFilled;
  } catch (err) {
    console.error('[conventionalReport] Error:', err);
    return undefined;
  }

};

const ledReport = async (
  maintenanceId: string,
  streetIds: string[]
) => {
  try {
    const data = await getLedMaintenancesByStreetId(maintenanceId, streetIds);
    if (!data) {
      console.log("Nenhum dado encontrado");
      return;
    }

    const {
      company,
      contract,
      maintenance,
      team,
      streets,
      total_by_item
    } = data;

    const templatePath = path.join(__dirname, '..', 'templates/maintenance', 'maintenance_led.html');
    const htmlTemplate = fs.readFileSync(templatePath, 'utf-8');

    let observations = '';
    const streetsLines = streets.map((line: any, index: number) => {
      if (line.comment) {
        observations += `${line.comment.trim().replace(/\.*$/, '')}. `;
      }
      return `
      <tr>
        <td>${index + 1}</td>
        <td>${line.address}</td>
        <td>${line.relay}</td>
        <td>${line.connection}</td>
        <td>${line.last_supply}</td>
        <td>${line.current_supply}</td>
        <td>${line.last_power}</td>
        <td>${line.power}</td>
        <td>${line.reason}</td>
      </tr>
    `;
    }).join('\n');

    const companyLogo = await getFileAsBase64(company.bucket, company.company_logo);
    const utcDateOfVisit: string = maintenance.date_of_visit;    
    const dateOfVisit = toSaoPauloTime(utcDateOfVisit);
    
    const hasPending = !!maintenance.pending_points;

    let htmlFilled = htmlTemplate
      .replace('{{BASE64_LOGO_IMAGE}}', companyLogo ?? '')
      .replace('{{CONTRACT_NUMBER}}', contract.contract_number ?? '')
      .replace('{{COMPANY_SOCIAL_REASON}}', company.social_reason ?? '')
      .replace('{{COMPANY_CNPJ}}', company.company_cnpj ?? '')
      .replace('{{COMPANY_ADDRESS}}', company.company_address ?? '')
      .replace('{{COMPANY_PHONE}}', company.company_phone ?? '')
      .replace('{{CONTRACTOR_SOCIAL_REASON}}', contract.contractor ?? '')
      .replace('{{CONTRACTOR_CNPJ}}', contract.cnpj ?? '')
      .replace('{{CONTRACTOR_ADDRESS}}', contract.address ?? '')
      .replace('{{CONTRACTOR_PHONE}}', contract.phone ?? '')
      .replace('{{STREET_LINES}}', streetsLines)
      .replace('{{RELAY_TOTAL}}', total_by_item.relay)
      .replace('{{CONNECTION_TOTAL}}', total_by_item.connection)
      .replace('{{OBSERVATIONS}}', observations)
      .replace('{{PENDING}}', hasPending ? 'checked' : '')
      .replace('{{NO_PENDING}}', hasPending ? '' : 'checked')
      .replace('{{PENDING_QUANTITY}}', maintenance.quantity_pending_points)
      .replace('{{LOCAL}}', maintenance.type)
      .replace('{{DATE_OF_VISIT}}', dateOfVisit.toFormat('dd/MM/yyyy'))
      .replace('{{ELECTRICIAN_NAME}}', `${team.electrician.name} ${team.electrician.last_name}`)
      .replace('{{DRIVER_NAME}}', `${team.driver.name} ${team.driver.last_name}`)

    if (maintenance.signature_uri) {
      const signatureImage = await getFileAsBase64(company.bucket, maintenance.signature_uri);
      const utcSignDate: string = maintenance.sign_date;
      const signDate = toSaoPauloTime(utcSignDate);
      
      const signSection =
        `
          <table >
              <thead>
                  <tr>
                      <th colspan="2" class="cell-title" style="text-align: center;">
                          ASSINATURA DO RESPONSÁVEL PELO ACOMPANHAMENTO DO SERVIÇO
                      </th>
                  </tr>
              </thead>
              <tbody>
                  <tr>
                      <td colspan="2" style="text-align: center; padding: 10px 0;">
                          <img src="data:image/png;base64,${signatureImage}" alt="Assinatura"
                              style="max-width: 250px; height: auto;">
                      </td>
                  </tr>
                  <tr>
                      <td colspan="2" style="text-align: center; padding: 4px;">
                          <p style="margin: 0; font-size: 10px; color: #555;">
                              Assinado digitalmente em: <strong>${signDate.toFormat("dd/MM/yyyy 'às' HH:mm")}</strong>
                          </p>
                      </td>
                  </tr>
                  <tr>
                      <td colspan="2">
                          <p class="label">Responsável:</p>
                          <p class="cell-text">${maintenance.responsible ?? ''}</p>
                      </td>
                  </tr>
              </tbody>
          </table>        
        `;

      htmlFilled = htmlFilled
        .replace('{{SIGN_SECTION}}', signSection)
    } else {
      htmlFilled = htmlFilled
        .replace('{{SIGN_SECTION}}', '')
    }

    return htmlFilled;
  } catch (err) {
    console.error('[conventionalReport] Error:', err);
    return undefined;
  }

};